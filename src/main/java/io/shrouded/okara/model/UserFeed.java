package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class UserFeed {

    private static final int MAX_FEED_SIZE = 500;
    private String id;
    private String userId;
    private LinkedList<FeedItem> items;
    private Timestamp lastUpdated;
    private String nextCursor;
    private String prevCursor;
    private Integer totalItems;
    private Timestamp createdAt;

    public UserFeed(String userId) {
        this.userId = userId;
        this.items = new LinkedList<>();
        this.createdAt = Timestamp.now();
        this.lastUpdated = Timestamp.now();
        this.totalItems = 0;
    }


    // Firestore compatibility: Ensure getter returns List interface
    public List<FeedItem> getItems() {
        return items;
    }

    // Firestore compatibility: Ensure setter accepts List interface
    public void setItems(List<FeedItem> items) {
        if (items instanceof LinkedList) {
            this.items = (LinkedList<FeedItem>) items;
        } else {
            this.items = new LinkedList<>(items);
        }
    }

    public void addItem(FeedItem item) {
        this.items.addFirst(item);
        this.lastUpdated = Timestamp.now();

        // FIFO: Remove oldest items efficiently with LinkedList
        while (this.items.size() > MAX_FEED_SIZE) {
            this.items.removeLast();
        }

        this.totalItems = this.items.size();
    }

    public void addItems(List<FeedItem> newItems) {
        // Sort new items by creation time (newest first)
        newItems.sort((a, b) -> b.getCreatedAt().toDate().compareTo(a.getCreatedAt().toDate()));

        // Add all new items to the front
        for (FeedItem item : newItems) {
            this.items.addFirst(item);
        }

        // Keep the entire list sorted (newest first)
        this.items.sort((a, b) -> b.getCreatedAt().toDate().compareTo(a.getCreatedAt().toDate()));

        this.lastUpdated = Timestamp.now();

        // FIFO: Remove oldest items efficiently
        while (this.items.size() > MAX_FEED_SIZE) {
            this.items.removeLast();
        }

        this.totalItems = this.items.size();
    }

    public List<FeedItem> getItemsPage(int offset, int limit) {
        int fromIndex = Math.min(offset, this.items.size());
        int toIndex = Math.min(offset + limit, this.items.size());

        if (fromIndex >= toIndex) {
            return new ArrayList<>();
        }

        return new ArrayList<>(this.items.subList(fromIndex, toIndex));
    }

    public List<FeedItem> getItemsSince(String sinceId, int limit) {
        if (sinceId == null || sinceId.isEmpty()) {
            return getItemsPage(0, limit);
        }

        int startIndex = 0;
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).getPostId().equals(sinceId)) {
                startIndex = i + 1;
                break;
            }
        }

        return getItemsPage(startIndex, limit);
    }

}