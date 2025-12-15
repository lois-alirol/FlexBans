import { useEffect, useState } from "react";

type Theme = 'dark' | 'light';

interface RevokeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onRevoke: (reason: string) => void;
  removerName: string;
  currentTheme: Theme;
  isSubmitting: boolean;
}

const RevokeModal: React.FC<RevokeModalProps> = ({ isOpen, onClose, onRevoke, removerName, currentTheme, isSubmitting }) => {
  const [removalReason, setRemovalReason] = useState('');

  useEffect(() => {
    const run = async () => {
      if (isOpen) {
        setRemovalReason('');
        document.body.classList.add('overflow-hidden');
      } else {
        document.body.classList.remove('overflow-hidden');
      }
    };

    run();

    return () => {
      document.body.classList.remove('overflow-hidden');
    };
  }, [isOpen]);


  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (removalReason.trim() && !isSubmitting) {
      onRevoke(removalReason);
    }
  };
  
  const modalBg = currentTheme === 'dark' ? 'bg-[#1c1c1c]' : 'bg-white';
  const inputBg = currentTheme === 'dark' ? 'bg-white text-black' : 'bg-white text-[#333333]';

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 backdrop-filter backdrop-blur-sm flex items-center justify-center z-200"
      onClick={onClose}
    >
      <div 
        className={`${modalBg} p-8 rounded-lg shadow-2xl max-w-md w-full`}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-xl font-semibold mb-4">Revoke Punishment</h2>
        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="identity" className="block font-medium mb-1">Who are you?</label>
            <input 
              type="text" 
              id="identity" 
              name="identity" 
              className="w-full p-2 border rounded-md text-[#333333] bg-gray-300 cursor-not-allowed" 
              value={removerName} 
              readOnly 
              required 
            />
          </div>
          <div className="mb-4">
            <label htmlFor="removal-reason" className="block font-medium mb-1">Reason for Removal</label>
            <textarea 
              id="removal-reason" 
              name="removal-reason" 
              className={`w-full p-2 border rounded-md ${inputBg}`} 
              rows={4} 
              value={removalReason}
              onChange={(e) => setRemovalReason(e.target.value)}
              required
              disabled={isSubmitting}
            ></textarea>
          </div>
          <div className="flex justify-end space-x-4">
            <button 
              type="button" 
              onClick={onClose} 
              className="bg-gray-500 hover:bg-gray-600 text-white py-2 px-4 rounded-lg transition duration-200"
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="bg-red-500 hover:bg-red-600 text-white py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
              disabled={isSubmitting || !removalReason.trim()}
            >
              {isSubmitting ? 'Submitting...' : 'Submit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RevokeModal;