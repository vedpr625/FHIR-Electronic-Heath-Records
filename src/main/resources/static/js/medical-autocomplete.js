/**
 * Medical Autocomplete System
 * Hybrid approach: ICD-10 from Clinicaltables API + SNOMED from local codes
 */

class MedicalAutocomplete {
    constructor() {
        this.activeRequest = null;
        this.cache = {
            icd10: {},
            snomed: {}
        };
    }

    /**
     * Search ICD-10 codes from local backend API (with fallback to Clinicaltables)
     */
    async searchICD10(searchTerm) {
        if (!searchTerm || searchTerm.length < 2) return [];
        
        // Check cache first
        if (this.cache.icd10[searchTerm]) {
            return this.cache.icd10[searchTerm];
        }

        // Cancel previous request if still pending
        if (this.activeRequest) {
            this.activeRequest.abort();
        }

        try {
            const controller = new AbortController();
            this.activeRequest = controller;

            // Try local backend first
            let results = [];
            try {
                const backendUrl = `/api/icd10/search?q=${encodeURIComponent(searchTerm)}`;
                const response = await fetch(backendUrl, { signal: controller.signal });
                if (response.ok) {
                    results = await response.json();
                    if (results && results.length > 0) {
                        console.log('ICD-10 results from backend:', results);
                        this.cache.icd10[searchTerm] = results;
                        return results;
                    }
                }
            } catch (backendError) {
                console.warn('Backend ICD-10 search failed, trying external API:', backendError);
            }

            // Fallback to external Clinicaltables API
            const url = `https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search?sf=code,name&terms=${encodeURIComponent(searchTerm)}&maxList=10`;
            const response = await fetch(url, { signal: controller.signal, timeout: 5000 });
            const data = await response.json();

            // Response format: [totalCount, searchTerm, unused, [[code, name], [code, name], ...]]
            results = (data[3] || []).map(item => ({
                code: item[0],
                display: item[1],
                system: 'ICD-10-CM'
            }));

            console.log('ICD-10 results from external API:', results);

            // Cache results
            this.cache.icd10[searchTerm] = results;
            return results;

        } catch (error) {
            if (error.name === 'AbortError') {
                return []; // Request was cancelled
            }
            console.error('ICD-10 search error:', error);
            return [];
        } finally {
            this.activeRequest = null;
        }
    }

    /**
     * Search SNOMED codes from local library
     */
    searchSNOMED(category, searchTerm) {
        if (!searchTerm || searchTerm.length < 2) return [];
        
        const cacheKey = `${category}:${searchTerm}`;
        if (this.cache.snomed[cacheKey]) {
            return this.cache.snomed[cacheKey];
        }

        // Check if searchSNOMED function exists
        if (typeof searchSNOMED !== 'function') {
            console.error('searchSNOMED function not found. Make sure snomed-codes.js is loaded.');
            return [];
        }

        const results = searchSNOMED(category, searchTerm).map(item => ({
            code: item.code,
            display: item.display,
            system: 'SNOMED-CT'
        }));

        console.log(`SNOMED search for "${searchTerm}" in category "${category}":`, results);
        this.cache.snomed[cacheKey] = results;
        return results;
    }

    /**
     * Initialize autocomplete on an input field
     */
    initAutocomplete(inputId, options = {}) {
        const input = document.getElementById(inputId);
        if (!input) {
            console.error(`Input field ${inputId} not found`);
            return;
        }

        const {
            type = 'icd10',           // 'icd10' or 'snomed'
            category = 'conditions',   // SNOMED category if type='snomed'
            codeFieldId = null,        // Hidden field for code
            onSelect = null,           // Callback when item selected
            minChars = 2,
            debounceMs = 300
        } = options;

        // Create autocomplete dropdown
        const dropdown = this.createDropdown(input);
        
        // Debounce search
        let debounceTimer;
        input.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            const value = e.target.value.trim();

            if (value.length < minChars) {
                dropdown.hide();
                return;
            }

            debounceTimer = setTimeout(async () => {
                console.log(`Autocomplete triggered: type=${type}, value="${value}", category="${category}"`);
                dropdown.showLoading();
                
                let results = [];
                if (type === 'icd10') {
                    results = await this.searchICD10(value);
                } else if (type === 'snomed') {
                    results = this.searchSNOMED(category, value);
                } else {
                    console.warn(`Unknown autocomplete type: ${type}`);
                }

                console.log(`Results for "${value}":`, results);
                dropdown.showResults(results, (selected) => {
                    input.value = selected.display;
                    if (codeFieldId) {
                        const codeField = document.getElementById(codeFieldId);
                        if (codeField) codeField.value = selected.code;
                    }
                    if (onSelect) onSelect(selected);
                    dropdown.hide();
                });
            }, debounceMs);
        });

        // Hide dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!input.contains(e.target) && !dropdown.element.contains(e.target)) {
                dropdown.hide();
            }
        });

        // Keyboard navigation
        input.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                dropdown.selectNext();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                dropdown.selectPrevious();
            } else if (e.key === 'Enter') {
                e.preventDefault();
                dropdown.selectCurrent();
            } else if (e.key === 'Escape') {
                dropdown.hide();
            }
        });
    }

    /**
     * Create autocomplete dropdown UI
     */
    createDropdown(inputElement) {
        const dropdown = document.createElement('div');
        dropdown.className = 'medical-autocomplete-dropdown';
        dropdown.style.display = 'none';
        
        // Position dropdown below input using fixed positioning (outside document flow)
        const updatePosition = () => {
            const rect = inputElement.getBoundingClientRect();
            const viewportWidth = window.innerWidth;
            const dropdownMinWidth = 280;
            const dropdownMaxWidth = 600;
            const preferredWidth = Math.max(rect.width, dropdownMinWidth);
            const finalWidth = Math.min(preferredWidth, dropdownMaxWidth);
            
            let left = rect.left;
            
            // Adjust if dropdown would go off-screen to the right
            if (left + finalWidth > viewportWidth) {
                left = Math.max(0, viewportWidth - finalWidth - 10);
            }
            
            dropdown.style.position = 'fixed';
            dropdown.style.top = `${rect.bottom + 4}px`;
            dropdown.style.left = `${left}px`;
            dropdown.style.width = `${finalWidth}px`;
            dropdown.style.zIndex = '10000';
        };

        document.body.appendChild(dropdown);

        let selectedIndex = -1;
        let currentResults = [];
        let onSelectCallback = null;

        return {
            element: dropdown,

            showLoading() {
                updatePosition();
                dropdown.innerHTML = '<div class="autocomplete-loading">Searching...</div>';
                dropdown.style.display = 'block';
            },

            showResults(results, onSelect) {
                currentResults = results;
                onSelectCallback = onSelect;
                selectedIndex = -1;
                updatePosition();

                if (results.length === 0) {
                    dropdown.innerHTML = '<div class="autocomplete-empty">No results found</div>';
                    dropdown.style.display = 'block';
                    return;
                }

                dropdown.innerHTML = results.map((item, index) => `
                    <div class="autocomplete-item" data-index="${index}">
                        <div class="autocomplete-code">${item.code}</div>
                        <div class="autocomplete-display">${item.display}</div>
                        <div class="autocomplete-system">${item.system}</div>
                    </div>
                `).join('');

                dropdown.style.display = 'block';

                // Add click handlers
                dropdown.querySelectorAll('.autocomplete-item').forEach((item, index) => {
                    item.addEventListener('click', () => {
                        if (onSelectCallback) onSelectCallback(results[index]);
                    });
                    item.addEventListener('mouseenter', () => {
                        this.selectIndex(index);
                    });
                });
            },

            hide() {
                dropdown.style.display = 'none';
                selectedIndex = -1;
            },

            selectNext() {
                if (currentResults.length === 0) return;
                selectedIndex = (selectedIndex + 1) % currentResults.length;
                this.highlightSelected();
            },

            selectPrevious() {
                if (currentResults.length === 0) return;
                selectedIndex = selectedIndex <= 0 ? currentResults.length - 1 : selectedIndex - 1;
                this.highlightSelected();
            },

            selectIndex(index) {
                selectedIndex = index;
                this.highlightSelected();
            },

            selectCurrent() {
                if (selectedIndex >= 0 && selectedIndex < currentResults.length) {
                    if (onSelectCallback) onSelectCallback(currentResults[selectedIndex]);
                }
            },

            highlightSelected() {
                dropdown.querySelectorAll('.autocomplete-item').forEach((item, index) => {
                    if (index === selectedIndex) {
                        item.classList.add('selected');
                        item.scrollIntoView({ block: 'nearest' });
                    } else {
                        item.classList.remove('selected');
                    }
                });
            }
        };
    }
}

// Global instance
const medicalAutocomplete = new MedicalAutocomplete();

// Verify all required functions are available
console.log('Medical Autocomplete loaded');
console.log('searchSNOMED function available:', typeof searchSNOMED === 'function');
console.log('SNOMED_CODES available:', typeof SNOMED_CODES !== 'undefined');
if (typeof SNOMED_CODES !== 'undefined' && SNOMED_CODES.medications) {
    console.log('Sample medications:', SNOMED_CODES.medications.slice(0, 3));
}
