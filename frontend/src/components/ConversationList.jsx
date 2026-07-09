export default function ConversationList({
  conversations,
  activeId,
  onSelect,
  onCreate,
  show,
  onClose,
}) {
  return (
    <div
      className={`
        fixed md:static inset-y-0 left-0 z-30 w-72 bg-gray-900 border-r border-gray-800 flex flex-col
        transition-transform duration-200
        ${show ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
        md:w-64
      `}
    >
      {/* Header */}
      <div className="p-4 border-b border-gray-800 flex items-center justify-between">
        <h2 className="text-white font-semibold text-sm">Chats</h2>
        <div className="flex gap-1">
          <button
            onClick={onCreate}
            className="text-gray-400 hover:text-white p-1 rounded-lg hover:bg-gray-800 transition-colors"
            title="New chat"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
          </button>
          <button
            onClick={onClose}
            className="md:hidden text-gray-400 hover:text-white p-1 rounded-lg hover:bg-gray-800 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      {/* List */}
      <div className="flex-1 overflow-y-auto">
        {conversations.length === 0 ? (
          <p className="text-gray-500 text-sm text-center mt-8 px-4">
            No conversations yet. Create one to start chatting.
          </p>
        ) : (
          conversations.map((c) => (
            <button
              key={c.id}
              onClick={() => { onSelect(c); onClose(); }}
              className={`w-full text-left px-4 py-3 border-b border-gray-800/50 transition-colors ${
                activeId === c.id
                  ? 'bg-indigo-600/20 border-l-2 border-l-indigo-500'
                  : 'hover:bg-gray-800/50'
              }`}
            >
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-gray-700 flex items-center justify-center text-xs font-medium text-gray-300 shrink-0">
                  {c.type === 'GROUP' ? '#' : '@'}
                </div>
                <div className="min-w-0">
                  <p className="text-white text-sm truncate">{c.name || 'Chat'}</p>
                  <p className="text-gray-500 text-xs">{c.type === 'GROUP' ? 'Group' : 'Private'}</p>
                </div>
              </div>
            </button>
          ))
        )}
      </div>
    </div>
  );
}
