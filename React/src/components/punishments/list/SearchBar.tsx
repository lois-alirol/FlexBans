import React from 'react';
import { FaSearch, FaFilter } from 'react-icons/fa';

interface SearchBarProps {
    searchTerm: string;
    onSearchChange: (value: string) => void;
    onFilterClick: () => void;
    placeholder: string;
    currentTheme: 'dark' | 'light';
    serverColor: string;
}

const SearchBar: React.FC<SearchBarProps> = ({
                                                 searchTerm,
                                                 onSearchChange,
                                                 onFilterClick,
                                                 placeholder,
                                                 currentTheme,
                                                 serverColor
                                             }) => {
    return (
        <div className="search mb-8 flex items-center gap-2">
            <div className="relative grow">
                <input
                    type="text"
                    placeholder={placeholder}
                    value={searchTerm}
                    onChange={(e) => onSearchChange(e.target.value)}
                    className={`w-full p-3.5 pl-12 rounded-xl shadow-lg focus:outline-none focus:ring-2 transition-all duration-300 ${
                        currentTheme === 'dark'
                            ? `bg-[#2c2c2c] border border-gray-700 text-white`
                            : `bg-white border border-gray-300 text-gray-800`
                    }`}
                    style={{
                        '--tw-ring-color': `${serverColor}66`
                    } as React.CSSProperties}
                />
                <FaSearch className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400" />
            </div>
            <button
                onClick={onFilterClick}
                className={`p-3.5 rounded-xl text-lg shadow-lg transition duration-300 transform hover:scale-[1.05] ${
                    currentTheme === 'dark' ? 'bg-[#2c2c2c] text-white hover:bg-[#383838]' : 'bg-white text-gray-800 hover:bg-gray-100'
                }`}
                aria-label="Filter punishments"
            >
                <FaFilter />
            </button>
        </div>
    );
};

export default SearchBar;