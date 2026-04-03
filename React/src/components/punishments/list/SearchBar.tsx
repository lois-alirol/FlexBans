import React from 'react';
import { FaSearch, FaFilter } from 'react-icons/fa';

interface SearchBarProps {
    searchTerm: string;
    onSearchChange: (value: string) => void;
    onFilterClick: () => void;
    placeholder: string;
}

const SearchBar: React.FC<SearchBarProps> = ({
                                                 searchTerm,
                                                 onSearchChange,
                                                 onFilterClick,
                                                 placeholder,
                                             }) => {

    const containerClasses = 'bg-surface border-surface-border text-text-primary placeholder-text-disabled'
    const buttonClasses = 'bg-surface border-surface-border text-text-primary hover:bg-surface-elevated'

    return (
        <div className="search mb-4 flex items-center gap-3">
            <div className="relative grow group">
                <input
                    type="text"
                    placeholder={placeholder}
                    value={searchTerm}
                    onChange={(e) => onSearchChange(e.target.value)}
                    className={`
                        w-full p-3.5 pl-12 rounded-xl backdrop-blur-md border outline-none
                        transition-all duration-300 transform focus:-translate-y-px focus:ring-2 focus:ring-server-color/30
                        ${containerClasses}
                    `}
                />
                <FaSearch className={`
                    absolute left-4 top-1/2 transform -translate-y-1/2 transition-colors duration-300
                    text-text-secondary group-focus-within:text-text-primary
                `} />
            </div>

            <button
                onClick={onFilterClick}
                className={`
                    p-4 rounded-xl text-lg backdrop-blur-md border
                    transition-all duration-300 transform hover:-translate-y-1 active:scale-95
                    ${buttonClasses}
                `}
                aria-label="Filter punishments"
            >
                <FaFilter className="text-text-secondary" />
            </button>
        </div>
    );
};

export default SearchBar;