import { useRef, useEffect } from 'react';

export default function MessageList({ messages, currentUserEmail }) {
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="flex-1 overflow-y-auto px-3 md:px-4 py-3 space-y-2">
      {messages.length === 0 && (
        <p className="text-gray-500 text-sm text-center mt-8">
          No messages yet. Say hello!
        </p>
      )}

      {messages.map((msg, i) => {
        const isMine = msg.senderEmail === currentUserEmail;
        return (
          <div key={msg.id || i} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`
                max-w-[80%] md:max-w-[70%] rounded-2xl px-4 py-2 text-sm break-words
                ${isMine
                  ? 'bg-indigo-600 text-white rounded-br-md'
                  : 'bg-gray-800 text-gray-100 rounded-bl-md'
                }
              `}
            >
              {!isMine && (
                <p className="text-indigo-400 text-xs font-medium mb-0.5 truncate">
                  {msg.senderEmail}
                </p>
              )}
              <p>{msg.content}</p>
              {msg.sentAt && (
                <p className={`text-[10px] mt-1 ${isMine ? 'text-indigo-200' : 'text-gray-500'}`}>
                  {new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </p>
              )}
            </div>
          </div>
        );
      })}
      <div ref={bottomRef} />
    </div>
  );
}
