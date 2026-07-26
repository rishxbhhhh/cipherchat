import { useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';

export default function useStomp(onMessage) {
  const clientRef = useRef(null);

  const connect = useCallback((token) => {
    if (clientRef.current?.active) return;

    const client = new Client({
      brokerURL: `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => console.debug('[STOMP]', str),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      console.log('[STOMP] Connected');
    };

    client.onStompError = (frame) => {
      console.error('[STOMP] Error:', frame.headers['message']);
    };

    client.activate();
    clientRef.current = client;
  }, []);

  const subscribe = useCallback((conversationId) => {
    const client = clientRef.current;
    if (!client?.active) return () => {};

    const subscription = client.subscribe(
      `/topic/conversation/${conversationId}`,
      (message) => {
        const body = JSON.parse(message.body);
        onMessage(conversationId, body);
      }
    );
    return () => subscription.unsubscribe();
  }, [onMessage]);

  const send = useCallback((conversationId, content) => {
    const client = clientRef.current;
    if (!client?.active) return;

    client.publish({
      destination: '/app/chat',
      body: JSON.stringify({ conversationId, content }),
    });
  }, []);

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
  }, []);

  return { connect, subscribe, send, disconnect };
}
