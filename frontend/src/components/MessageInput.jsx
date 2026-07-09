import { useState } from 'react';

export default function MessageInput({ onSend }) {
  const [text, setText] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setText('');
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="border-t border-gray-800 px-3 md:px-4 py-3 flex gap-2 bg-gray-900"
    >
      <input
        type="text"
        value={text}
        onChange={(e) => setText(e.target.value)}
        className="flex-1 bg-gray-800 border border-gray-700 rounded-full px-4 py-2.5 text-white text-sm focus:outline-none focus:border-indigo-500 transition-colors"
        placeholder="Type a message..."
        autoFocus
      />
      <button
        type="submit"
        disabled={!text.trim()}
        className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white rounded-full p-2.5 transition-colors shrink-0"
      >
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
        </svg>
      </button>
    </form>
  );
}
