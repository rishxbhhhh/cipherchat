import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

export default function Admin() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [toggling, setToggling] = useState(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams({ page, size: '15' });
      if (search.trim()) params.set('search', search.trim());

      const res = await api.get(`/admin/users?${params}`);
      setUsers(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch {
      setError('Failed to load users. You may not have admin access.');
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const handleToggle = async (userId) => {
    setToggling(userId);
    try {
      const res = await api.put(`/admin/users/${userId}/toggle`);
      setUsers((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, enabled: res.data.enabled } : u))
      );
    } catch (err) {
      setError(err.response?.data?.error || 'Toggle failed.');
    } finally {
      setToggling(null);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchUsers();
  };

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col">
      {/* Top bar */}
      <div className="h-14 border-b border-gray-800 flex items-center px-4 gap-4 shrink-0 bg-gray-900">
        <h1 className="text-white font-semibold text-sm">Admin Panel</h1>
        <span className="text-gray-500 text-xs hidden sm:block">User Management</span>
        <button
          onClick={() => navigate('/chat')}
          className="ml-auto text-gray-400 hover:text-white text-sm transition-colors"
        >
          Back to Chat
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 max-w-4xl mx-auto w-full px-4 py-6">
        {/* Search */}
        <form onSubmit={handleSearch} className="flex gap-2 mb-4">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by email..."
            className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2.5 text-white text-sm focus:outline-none focus:border-indigo-500 transition-colors"
          />
          <button
            type="submit"
            className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2.5 rounded-lg text-sm font-medium transition-colors"
          >
            Search
          </button>
          {search && (
            <button
              type="button"
              onClick={() => { setSearch(''); setPage(0); }}
              className="text-gray-400 hover:text-white text-sm px-2 transition-colors"
            >
              Clear
            </button>
          )}
        </form>

        {error && (
          <div className="bg-red-900/30 border border-red-800 text-red-300 text-sm rounded-lg px-4 py-3 mb-4">
            {error}
          </div>
        )}

        {/* User list */}
        <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden">
          {/* Table header */}
          <div className="grid grid-cols-[1fr_100px_90px] px-4 py-3 border-b border-gray-800 text-xs text-gray-500 uppercase tracking-wider">
            <span>Email</span>
            <span>Role</span>
            <span className="text-right">Status</span>
          </div>

          {loading ? (
            <div className="px-4 py-8 text-center text-gray-500 text-sm">Loading...</div>
          ) : users.length === 0 ? (
            <div className="px-4 py-8 text-center text-gray-500 text-sm">
              {search ? 'No users match your search.' : 'No users found.'}
            </div>
          ) : (
            users.map((user) => (
              <div
                key={user.id}
                className="grid grid-cols-[1fr_100px_90px] px-4 py-3 border-b border-gray-800/50 items-center text-sm hover:bg-gray-800/30 transition-colors"
              >
                <div className="text-white truncate">
                  <span>{user.email}</span>
                  {user.role === 'ADMIN' && (
                    <span className="ml-2 px-1.5 py-0.5 bg-indigo-600/30 text-indigo-300 text-[10px] rounded">
                      ADMIN
                    </span>
                  )}
                </div>
                <span className="text-gray-400 text-xs">{user.role}</span>
                <div className="flex justify-end">
                  {user.role === 'ADMIN' ? (
                    <span className="text-gray-500 text-xs italic">Protected</span>
                  ) : (
                    <button
                      onClick={() => handleToggle(user.id)}
                      disabled={toggling === user.id}
                      className={`
                        px-3 py-1 rounded text-xs font-medium transition-colors
                        ${user.enabled
                          ? 'bg-green-900/40 text-green-400 hover:bg-red-900/40 hover:text-red-400'
                          : 'bg-red-900/40 text-red-400 hover:bg-green-900/40 hover:text-green-400'
                        }
                        disabled:opacity-50
                      `}
                    >
                      {toggling === user.id ? '...' : user.enabled ? 'Enabled' : 'Disabled'}
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 mt-4">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 text-sm text-gray-400 hover:text-white disabled:opacity-30 transition-colors"
            >
              Previous
            </button>
            <span className="text-gray-500 text-sm">
              Page {page + 1} of {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page + 1 >= totalPages}
              className="px-3 py-1.5 text-sm text-gray-400 hover:text-white disabled:opacity-30 transition-colors"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
