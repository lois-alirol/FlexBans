import React, {useState} from 'react';
import {Link} from 'react-router-dom';
import {FaSearch} from 'react-icons/fa';

const SearchInput: React.FC<{
    label: string,
    placeholder: string,
    icon: React.ReactNode,
    id: string,
    route: string,
}> = ({ label, placeholder, icon, id, route }) => {
    const [searchValue, setSearchValue] = useState('');
    const searchPath = `/${route}/${encodeURIComponent(searchValue.trim())}`;
    const isDisabled = searchValue.trim() === '';

    return (
        <div className="mb-5 animate-in fade-in zoom-in duration-300">
            <label htmlFor={id} className="block text-[10px] font-bold uppercase tracking-widest text-sidebar-text-secondary mb-1.5 px-1">
                {label}
            </label>
            <div className="relative group">
                <div className="absolute left-3 top-1/2 -translate-y-1/2 z-10 text-sidebar-text-disabled group-focus-within:text-sidebar-text-active transition-colors">
                    {icon}
                </div>
                <input
                    type="text"
                    id={id}
                    placeholder={placeholder}
                    value={searchValue}
                    onChange={(e) => setSearchValue(e.target.value)}
                    className={`w-full py-2.5 pl-9 pr-10 rounded-xl text-sm transition-all border 
                                outline-none bg-sidebar-surface border-border-c text-sidebar-text-active 
                                placeholder-sidebar-text-disabled focus:ring-2 focus:ring-server-color/30
                                focus:-translate-y-px`}
                />
                <Link
                    to={searchPath}
                    onClick={(e) => isDisabled && e.preventDefault()}
                    className={`absolute right-2 top-1/2 -translate-y-1/2 p-1.5 rounded-lg transition-all ${isDisabled ? 'opacity-0 scale-75' : 'opacity-100 scale-100 bg-sidebar-surface-elevated text-sidebar-text-active'}`}
                >
                    <FaSearch size={12} />
                </Link>
            </div>
        </div>
    );
};

export default SearchInput;