import { useRef, useEffect } from 'react';

function formatDate(dateStr) {
  const d = new Date(dateStr);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today - 86400000);
  const msgDay = new Date(d.getFullYear(), d.getMonth(), d.getDate());

  if (msgDay.getTime() === today.getTime()) return 'Today';
  if (msgDay.getTime() === yesterday.getTime()) return 'Yesterday';
  return d.toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatTime(dateStr) {
  return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export default function MessageList({ messages, currentUserEmail }) {
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Group messages: date separators + merge consecutive same-sender
  const groups = [];
  let lastDate = null;
  let lastSender = null;
  let currentGroup = null;

  for (const msg of messages) {
    const msgDate = msg.sentAt ? formatDate(msg.sentAt) : null;

    // Insert date separator
    if (msgDate && msgDate !== lastDate) {
      lastDate = msgDate;
      groups.push({ type: 'date', date: msgDate });
      lastSender = null;
      currentGroup = null;
    }

    // Start new sender group
    if (msg.senderEmail !== lastSender) {
      lastSender = msg.senderEmail;
      currentGroup = { type: 'messages', sender: msg.senderEmail, isMine: msg.senderEmail === currentUserEmail, items: [] };
      groups.push(currentGroup);
    }

    currentGroup?.items.push(msg);
  }

  return (
    <div className="flex-1 overflow-y-auto px-3 md:px-4 py-3 min-h-0">
      {groups.length === 0 && (
        <p className="text-gray-500 text-sm text-center mt-8">
          No messages yet. Say hello!
        </p>
      )}

      {groups.map((g, gi) => {
        if (g.type === 'date') {
          return (
            <div key={`date-${gi}`} className="flex items-center justify-center my-3">
              <span className="bg-gray-800 text-gray-400 text-xs px-3 py-1 rounded-full">
                {g.date}
              </span>
            </div>
          );
        }

        // Message group
        return (
          <div key={`group-${gi}`} className={`flex flex-col mb-1 ${g.isMine ? 'items-end' : 'items-start'}`}>
            {g.items.map((msg, mi) => {
              const isFirst = mi === 0;
              const isLast = mi === g.items.length - 1;
              return (
                <div
                  key={msg.id || mi}
                  className={`
                    max-w-[80%] md:max-w-[70%] px-4 py-1.5 text-sm break-words
                    ${g.isMine
                      ? 'bg-indigo-600 text-white'
                      : 'bg-gray-800 text-gray-100'
                    }
                    ${isFirst && isLast ? 'rounded-2xl' : ''}
                    ${isFirst && !isLast ? 'rounded-t-2xl rounded-br-xl rounded-bl-md' : ''}
                    ${!isFirst && !isLast ? 'rounded-none' : ''}
                    ${!isFirst && isLast ? 'rounded-b-2xl rounded-tr-xl rounded-tl-md' : ''}
                    ${g.isMine
                      ? (isLast ? 'rounded-br-md' : '')
                      : (isLast ? 'rounded-bl-md' : '')
                    }
                    ${mi > 0 ? 'mt-[1px]' : 'mt-0'}
                  `}
                >
                  {/* Sender name only on first message */}
                  {isFirst && !g.isMine && (
                    <p className="text-indigo-400 text-xs font-medium mb-0.5">
                      {g.sender}
                    </p>
                  )}
                  <p>{msg.content}</p>
                  {/* Timestamp only on last message */}
                  {isLast && msg.sentAt && (
                    <p className={`text-[10px] mt-0.5 ${g.isMine ? 'text-indigo-200' : 'text-gray-500'}`}>
                      {formatTime(msg.sentAt)}
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        );
      })}
      <div ref={bottomRef} />
    </div>
  );
}
