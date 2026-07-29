package com.ordering.service;

import com.ordering.dto.OrderDTO;
import com.ordering.dto.OrderItemDTO;
import com.ordering.dto.WaiterCallDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyOrderCreated(OrderDTO order) {
        messagingTemplate.convertAndSend("/topic/admin", Map.of("event", "order.created", "data", order));
        messagingTemplate.convertAndSend("/topic/table/" + order.getTableId(), Map.of("event", "order.created", "data", order));
    }

    public void notifyItemStatusUpdated(Long orderId, Long tableId, OrderItemDTO item) {
        messagingTemplate.convertAndSend("/topic/admin", Map.of("event", "order.item-status-updated", "data", Map.of("orderId", orderId, "item", item)));
        messagingTemplate.convertAndSend("/topic/table/" + tableId, Map.of("event", "order.item-status-updated", "data", Map.of("orderId", orderId, "item", item)));
    }

    public void notifyOrderCompleted(Long orderId, Long tableId) {
        messagingTemplate.convertAndSend("/topic/admin", Map.of("event", "order.completed", "data", Map.of("orderId", orderId)));
        messagingTemplate.convertAndSend("/topic/table/" + tableId, Map.of("event", "order.completed", "data", Map.of("orderId", orderId)));
    }

    public void notifyWaiterCallCreated(WaiterCallDTO call) {
        messagingTemplate.convertAndSend("/topic/admin", Map.of("event", "waiter-call.created", "data", call));
    }

    public void notifyWaiterCallHandled(Long callId) {
        messagingTemplate.convertAndSend("/topic/admin", Map.of("event", "waiter-call.handled", "data", Map.of("callId", callId)));
    }
}
