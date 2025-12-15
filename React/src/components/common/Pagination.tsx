import React from 'react';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';
import { useServerConfig } from '../../hooks/useServerConfig';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  currentTheme: 'light' | 'dark';
}

const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  currentTheme,
}) => {
  const { serverConfig } = useServerConfig();

  if (totalPages <= 1) {
    return null;
  }

  const isDark = currentTheme === 'dark';
  const primaryColor = serverConfig.serverColor; 

  const getPageNumbers = () => {
    const pageNumbers: (number | '...')[] = [];
    const maxPagesToShow = 5;

    if (totalPages <= maxPagesToShow) {
      for (let i = 1; i <= totalPages; i++) {
        pageNumbers.push(i);
      }
    } else {
      pageNumbers.push(1);
      
      const start = Math.max(2, currentPage - 1);
      const end = Math.min(totalPages - 1, currentPage + 1);

      if (start > 2) pageNumbers.push('...');
      
      for (let i = start; i <= end; i++) {
        if (i !== 1 && i !== totalPages) {
          pageNumbers.push(i);
        }
      }
      
      if (end < totalPages - 1) pageNumbers.push('...');
      
      if (totalPages > 1 && !pageNumbers.includes(totalPages)) {
        pageNumbers.push(totalPages);
      }
    }
    
    return pageNumbers.filter((item, index, self) => 
        !(item === '...' && self[index - 1] === '...') &&
        !(item === 1 && self[index - 1] === 1) &&
        !(item === totalPages && self[index - 1] === totalPages)
    );
  };
  
  const pageNumbers = getPageNumbers();

  const baseStyle = `px-3 py-1 rounded-md text-sm font-medium transition duration-300 shadow-sm`;
  
  const defaultStyle = isDark 
    ? `bg-[#2c2c2c] text-white hover:bg-[#383838]` 
    : `bg-white text-gray-800 hover:bg-gray-100 border border-gray-300`;
    
  const activeStyle = `bg-[${primaryColor}] text-white shadow-lg`;

  return (
    <div className="flex justify-between items-center mt-6">
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
        className={`flex items-center gap-1 ${baseStyle} ${isDark ? 'text-gray-400 disabled:opacity-50' : 'text-gray-600 disabled:opacity-50'} ${defaultStyle}`}
      >
        <FaChevronLeft className="h-3 w-3" /> Previous
      </button>

      <div className="flex space-x-1">
        {pageNumbers.map((page, index) => {
          if (page === '...') {
            return (
              <span key={`dots-${index}`} className="px-3 py-1 text-sm text-gray-500 dark:text-gray-400">
                ...
              </span>
            );
          }
          
          const isCurrent = page === currentPage;
          
          return (
            <button
              key={page}
              onClick={() => onPageChange(page as number)}
              className={`${baseStyle} ${isCurrent ? activeStyle : defaultStyle}`}
              aria-current={isCurrent ? 'page' : undefined}
            >
              {page}
            </button>
          );
        })}
      </div>

      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
        className={`flex items-center gap-1 ${baseStyle} ${isDark ? 'text-white disabled:opacity-50' : 'text-gray-800 disabled:opacity-50'} ${defaultStyle}`}
      >
        Next <FaChevronRight className="h-3 w-3" />
      </button>
    </div>
  );
};

export default Pagination;