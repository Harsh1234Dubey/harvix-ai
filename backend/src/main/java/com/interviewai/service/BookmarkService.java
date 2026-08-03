package com.interviewai.service;

import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.Bookmark;
import com.interviewai.domain.User;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.exception.DuplicateResourceException;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public MessageResponse add(User user, String entityType, Long entityId) {
        if (bookmarkRepository.existsByUserIdAndEntityTypeAndEntityId(user.getId(), entityType, entityId)) {
            throw new DuplicateResourceException("Already bookmarked");
        }
        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setEntityType(entityType);
        bookmark.setEntityId(entityId);
        bookmarkRepository.save(bookmark);
        return MessageResponse.of("Bookmarked successfully");
    }

    @Transactional
    public MessageResponse remove(User user, String entityType, Long entityId) {
        bookmarkRepository.deleteByUserIdAndEntityTypeAndEntityId(user.getId(), entityType, entityId);
        return MessageResponse.of("Bookmark removed");
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(User user, String entityType, Long entityId) {
        return bookmarkRepository.existsByUserIdAndEntityTypeAndEntityId(user.getId(), entityType, entityId);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(User user, String entityType, int page, int size) {
        Pageable pageable = PageableUtils.build(page, size, "createdAt:desc");
        Page<Bookmark> bookmarks = bookmarkRepository.findByUserIdAndEntityType(user.getId(), entityType, pageable);
        List<Map<String, Object>> content = bookmarks.getContent().stream()
                .map(b -> Map.<String, Object>of(
                        "id", b.getId(),
                        "entityType", b.getEntityType(),
                        "entityId", b.getEntityId(),
                        "createdAt", b.getCreatedAt().toString()))
                .toList();
        return PageResponse.from(bookmarks, content);
    }
}
