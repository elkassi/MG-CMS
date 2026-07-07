import React, { Component } from 'react';
import axios from 'axios';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFolder, faSync, faFile, faFolderOpen, faDownload, faEye, faSearch, faTimes, faChevronLeft, faChevronRight, faArrowUp, faArrowDown } from '@fortawesome/free-solid-svg-icons';
import { Modal } from 'react-bootstrap';

export default class MachineLog extends Component {
    constructor(props) {
        super(props);
        this.state = {
            folders: [],
            loading: false,
            error: null,
            selectedFolder: null,
            folderContents: [],
            loadingContents: false,
            // Pagination
            currentPage: 0,
            pageSize: 100,
            totalFiles: 0,
            // Search
            fileNameSearch: '',
            prefixSearch: '',
            // File viewer
            showFileViewer: false,
            fileContent: '',
            loadingFile: false,
            viewingFileName: '',
            // File search (Ctrl+F style)
            fileSearchText: '',
            fileSearchResults: [],
            currentSearchIndex: 0
        };
        this.fileContentRef = React.createRef();
    }

    componentDidMount() {
        this.loadFolders();
    }

    loadFolders = () => {
        this.setState({ loading: true, error: null });
        axios.get('/api/machineLog/folderNames')
            .then(response => {
                this.setState({ folders: response.data, loading: false });
            })
            .catch(error => {
                this.setState({ 
                    error: error.response?.data || 'Error loading folders', 
                    loading: false 
                });
            });
    }

    loadFolderContents = (folderName, page = 0) => {
        this.setState({ 
            selectedFolder: folderName, 
            loadingContents: true,
            folderContents: [],
            currentPage: page
        });
        
        const { pageSize, fileNameSearch, prefixSearch } = this.state;
        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', pageSize);
        if (fileNameSearch) params.append('fileName', fileNameSearch);
        if (prefixSearch) params.append('prefix', prefixSearch);
        
        axios.get(`/api/machineLog/folder/${folderName}?${params.toString()}`)
            .then(response => {
                this.setState({ 
                    folderContents: response.data.files || response.data,
                    totalFiles: response.data.totalFiles || response.data.length,
                    loadingContents: false 
                });
            })
            .catch(error => {
                this.setState({ 
                    error: error.response?.data || 'Error loading folder contents', 
                    loadingContents: false 
                });
            });
    }

    downloadFile = (fileName) => {
        const { selectedFolder } = this.state;
        axios({
            url: `/api/machineLog/download/${selectedFolder}/${fileName}`,
            method: 'GET',
            responseType: 'blob',
        }).then((response) => {
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', fileName);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        }).catch(error => {
            alert('Error downloading file: ' + (error.response?.data || error.message));
        });
    }

    viewFile = (fileName) => {
        const { selectedFolder } = this.state;
        this.setState({ 
            showFileViewer: true, 
            loadingFile: true, 
            viewingFileName: fileName,
            fileContent: '',
            fileSearchText: '',
            fileSearchResults: [],
            currentSearchIndex: 0
        });
        
        axios.get(`/api/machineLog/read/${selectedFolder}/${fileName}`)
            .then(response => {
                this.setState({ 
                    fileContent: response.data,
                    loadingFile: false 
                });
            })
            .catch(error => {
                this.setState({ 
                    fileContent: 'Error reading file: ' + (error.response?.data || error.message),
                    loadingFile: false 
                });
            });
    }

    handleFileSearch = (searchText) => {
        this.setState({ fileSearchText: searchText }, () => {
            if (!searchText) {
                this.setState({ fileSearchResults: [], currentSearchIndex: 0 });
                return;
            }
            
            const { fileContent } = this.state;
            const regex = new RegExp(searchText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi');
            const results = [];
            let match;
            
            while ((match = regex.exec(fileContent)) !== null) {
                results.push(match.index);
            }
            
            this.setState({ 
                fileSearchResults: results, 
                currentSearchIndex: results.length > 0 ? 0 : -1 
            }, () => {
                if (results.length > 0) {
                    this.scrollToSearchResult(0);
                }
            });
        });
    }

    scrollToSearchResult = (index) => {
        const { fileSearchResults, fileContent } = this.state;
        if (index < 0 || index >= fileSearchResults.length) return;
        
        this.setState({ currentSearchIndex: index });
        
        // Calculate line number and scroll
        const position = fileSearchResults[index];
        const linesBefore = fileContent.substring(0, position).split('\n').length;
        
        if (this.fileContentRef.current) {
            const lineHeight = 20; // Approximate line height
            this.fileContentRef.current.scrollTop = (linesBefore - 5) * lineHeight;
        }
    }

    navigateSearch = (direction) => {
        const { currentSearchIndex, fileSearchResults } = this.state;
        let newIndex = currentSearchIndex + direction;
        
        if (newIndex < 0) newIndex = fileSearchResults.length - 1;
        if (newIndex >= fileSearchResults.length) newIndex = 0;
        
        this.scrollToSearchResult(newIndex);
    }

    getHighlightedContent = () => {
        const { fileContent, fileSearchText, fileSearchResults, currentSearchIndex } = this.state;
        
        if (!fileSearchText || fileSearchResults.length === 0) {
            return this.escapeHtml(fileContent);
        }
        
        // Escape HTML first
        let result = this.escapeHtml(fileContent);
        const escapedSearchText = this.escapeHtml(fileSearchText);
        const regex = new RegExp(`(${escapedSearchText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
        
        let matchIndex = 0;
        result = result.replace(regex, (match) => {
            const isCurrent = matchIndex === currentSearchIndex;
            matchIndex++;
            return `<mark style="background-color: ${isCurrent ? '#ff9800' : '#ffeb3b'}; padding: 0 2px;">${match}</mark>`;
        });
        
        return result;
    }

    escapeHtml = (text) => {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    getTotalPages = () => {
        const { totalFiles, pageSize } = this.state;
        return Math.ceil(totalFiles / pageSize);
    }

    renderPagination = () => {
        const { currentPage, totalFiles, pageSize, selectedFolder } = this.state;
        const totalPages = this.getTotalPages();
        
        if (totalPages <= 1) return null;
        
        return (
            <div className="d-flex justify-content-between align-items-center p-2 bg-light border-top">
                <small>
                    Affichage {currentPage * pageSize + 1} - {Math.min((currentPage + 1) * pageSize, totalFiles)} sur {totalFiles} fichiers
                </small>
                <div className="btn-group btn-group-sm">
                    <button 
                        className="btn btn-outline-primary"
                        disabled={currentPage === 0}
                        onClick={() => this.loadFolderContents(selectedFolder, currentPage - 1)}
                    >
                        <FontAwesomeIcon icon={faChevronLeft} />
                    </button>
                    <span className="btn btn-light disabled">
                        Page {currentPage + 1} / {totalPages}
                    </span>
                    <button 
                        className="btn btn-outline-primary"
                        disabled={currentPage >= totalPages - 1}
                        onClick={() => this.loadFolderContents(selectedFolder, currentPage + 1)}
                    >
                        <FontAwesomeIcon icon={faChevronRight} />
                    </button>
                </div>
            </div>
        );
    }

    renderFileViewer = () => {
        const { showFileViewer, loadingFile, viewingFileName, 
                fileSearchText, fileSearchResults, currentSearchIndex } = this.state;
        
        return (
            <Modal
                show={showFileViewer}
                onHide={() => this.setState({ showFileViewer: false, fileContent: '', fileSearchText: '' })}
                size="xl"
                dialogClassName="modal-90w"
            >
                <Modal.Header className="bg-dark text-white">
                    <Modal.Title>
                        <FontAwesomeIcon icon={faFile} className="mr-2" />
                        {viewingFileName}
                    </Modal.Title>
                    <div className="d-flex align-items-center">
                        {/* Search box */}
                        <div className="input-group input-group-sm mr-3" style={{ width: '300px' }}>
                            <div className="input-group-prepend">
                                <span className="input-group-text">
                                    <FontAwesomeIcon icon={faSearch} />
                                </span>
                            </div>
                            <input
                                type="text"
                                className="form-control"
                                placeholder="Rechercher (Ctrl+F)"
                                value={fileSearchText}
                                onChange={(e) => this.handleFileSearch(e.target.value)}
                            />
                            {fileSearchResults.length > 0 && (
                                <>
                                    <div className="input-group-append">
                                        <span className="input-group-text">
                                            {currentSearchIndex + 1}/{fileSearchResults.length}
                                        </span>
                                    </div>
                                    <div className="input-group-append">
                                        <button 
                                            className="btn btn-outline-light"
                                            onClick={() => this.navigateSearch(-1)}
                                        >
                                            <FontAwesomeIcon icon={faArrowUp} />
                                        </button>
                                        <button 
                                            className="btn btn-outline-light"
                                            onClick={() => this.navigateSearch(1)}
                                        >
                                            <FontAwesomeIcon icon={faArrowDown} />
                                        </button>
                                    </div>
                                </>
                            )}
                        </div>
                        <button 
                            className="btn btn-success btn-sm mr-2"
                            onClick={() => this.downloadFile(viewingFileName)}
                        >
                            <FontAwesomeIcon icon={faDownload} /> Télécharger
                        </button>
                        <button 
                            type="button" 
                            className="close text-white ml-2" 
                            onClick={() => this.setState({ showFileViewer: false })}
                        >
                            <FontAwesomeIcon icon={faTimes} />
                        </button>
                    </div>
                </Modal.Header>
                <Modal.Body style={{ padding: 0 }}>
                    {loadingFile ? (
                        <div className="text-center p-5">
                            <FontAwesomeIcon icon={faSync} spin size="3x" />
                            <p className="mt-3">Chargement du fichier...</p>
                        </div>
                    ) : (
                        <pre 
                            ref={this.fileContentRef}
                            style={{ 
                                maxHeight: '70vh', 
                                overflow: 'auto', 
                                margin: 0, 
                                padding: '15px',
                                backgroundColor: '#f8f9fa',
                                fontSize: '12px',
                                lineHeight: '20px',
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-all'
                            }}
                            dangerouslySetInnerHTML={{ __html: this.getHighlightedContent() }}
                        />
                    )}
                </Modal.Body>
            </Modal>
        );
    }

    formatFileSize = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    getTimeDiff = (timestamp) => {
        const now = Date.now();
        const diff = now - timestamp;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);

        if (days > 0) return `${days} jour(s)`;
        if (hours > 0) return `${hours} heure(s)`;
        if (minutes > 0) return `${minutes} minute(s)`;
        return 'À l\'instant';
    }

    render() {
        const { folders, loading, error, selectedFolder, folderContents, loadingContents,
                fileNameSearch, prefixSearch } = this.state;

        return (
            <div className="container-fluid" style={{ padding: '20px' }}>
                {this.renderFileViewer()}
                
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <h2>
                        <FontAwesomeIcon icon={faFolder} className="mr-2" />
                        Machine Log - Lectra History
                    </h2>
                    <button 
                        className="btn btn-primary"
                        onClick={this.loadFolders}
                        disabled={loading}
                    >
                        <FontAwesomeIcon icon={faSync} spin={loading} className="mr-2" />
                        Rafraîchir
                    </button>
                </div>

                {error && (
                    <div className="alert alert-danger">{error}</div>
                )}

                <div className="row">
                    {/* Folders List */}
                    <div className="col-md-3">
                        <div className="card">
                            <div className="card-header bg-dark text-white">
                                <FontAwesomeIcon icon={faFolder} className="mr-2" />
                                Tables de Production ({folders.length})
                            </div>
                            <div className="card-body" style={{ maxHeight: '70vh', overflowY: 'auto', padding: 0 }}>
                                {loading ? (
                                    <div className="text-center p-4">
                                        <FontAwesomeIcon icon={faSync} spin size="2x" />
                                    </div>
                                ) : (
                                    <table className="table table-hover table-striped mb-0">
                                        <thead className="thead-light">
                                            <tr>
                                                <th>Machine</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {folders.map((folder, index) => (
                                                <tr 
                                                    key={index}
                                                    onClick={() => this.loadFolderContents(folder.name, 0)}
                                                    style={{ 
                                                        cursor: 'pointer',
                                                        backgroundColor: selectedFolder === folder.name ? '#e3f2fd' : 'inherit'
                                                    }}
                                                >
                                                    <td>
                                                        <FontAwesomeIcon 
                                                            icon={selectedFolder === folder.name ? faFolderOpen : faFolder} 
                                                            className="mr-2 text-warning" 
                                                        />
                                                        <strong>{folder.name}</strong>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Folder Contents */}
                    <div className="col-md-9">
                        {selectedFolder && (
                            <div className="card">
                                <div className="card-header bg-primary text-white">
                                    <div className="d-flex justify-content-between align-items-center">
                                        <span>
                                            <FontAwesomeIcon icon={faFolderOpen} className="mr-2" />
                                            Contenu de {selectedFolder}
                                        </span>
                                    </div>
                                </div>
                                
                                {/* Search bar */}
                                <div className="card-body border-bottom p-2">
                                    <div className="row">
                                        <div className="col-md-5">
                                            <div className="input-group input-group-sm">
                                                <div className="input-group-prepend">
                                                    <span className="input-group-text">
                                                        <FontAwesomeIcon icon={faSearch} />
                                                    </span>
                                                </div>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    placeholder="Rechercher par nom..."
                                                    value={fileNameSearch}
                                                    onChange={(e) => this.setState({ fileNameSearch: e.target.value })}
                                                    onKeyUp={(e) => {
                                                        if (e.key === 'Enter') {
                                                            this.loadFolderContents(selectedFolder, 0);
                                                        }
                                                    }}
                                                />
                                            </div>
                                        </div>
                                        <div className="col-md-5">
                                            <div className="input-group input-group-sm">
                                                <div className="input-group-prepend">
                                                    <span className="input-group-text">Préfixe</span>
                                                </div>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    placeholder="Filtrer par préfixe..."
                                                    value={prefixSearch}
                                                    onChange={(e) => this.setState({ prefixSearch: e.target.value })}
                                                    onKeyUp={(e) => {
                                                        if (e.key === 'Enter') {
                                                            this.loadFolderContents(selectedFolder, 0);
                                                        }
                                                    }}
                                                />
                                            </div>
                                        </div>
                                        <div className="col-md-2">
                                            <button 
                                                className="btn btn-primary btn-sm btn-block"
                                                onClick={() => this.loadFolderContents(selectedFolder, 0)}
                                            >
                                                <FontAwesomeIcon icon={faSearch} /> Rechercher
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                
                                <div className="card-body" style={{ maxHeight: '55vh', overflowY: 'auto', padding: 0 }}>
                                    {loadingContents ? (
                                        <div className="text-center p-4">
                                            <FontAwesomeIcon icon={faSync} spin size="2x" />
                                        </div>
                                    ) : (
                                        <table className="table table-hover table-striped mb-0 table-sm">
                                            <thead className="thead-light" style={{ position: 'sticky', top: 0 }}>
                                                <tr>
                                                    <th>Nom</th>
                                                    <th>Taille</th>
                                                    <th>Dernière modification</th>
                                                    <th>Il y a</th>
                                                    <th style={{ width: '100px' }}>Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {folderContents.map((file, index) => (
                                                    <tr key={index}>
                                                        <td>
                                                            <FontAwesomeIcon 
                                                                icon={file.isDirectory ? faFolder : faFile} 
                                                                className={`mr-2 ${file.isDirectory ? 'text-warning' : 'text-secondary'}`}
                                                            />
                                                            {file.name}
                                                        </td>
                                                        <td>{file.isDirectory ? '-' : this.formatFileSize(file.size)}</td>
                                                        <td style={{ fontSize: '11px' }}>{file.lastModified}</td>
                                                        <td style={{ fontSize: '11px' }}>{this.getTimeDiff(file.lastModifiedTimestamp)}</td>
                                                        <td>
                                                            {!file.isDirectory && (
                                                                <div className="btn-group btn-group-sm">
                                                                    <button 
                                                                        className="btn btn-outline-primary"
                                                                        title="Voir le contenu"
                                                                        onClick={() => this.viewFile(file.name)}
                                                                    >
                                                                        <FontAwesomeIcon icon={faEye} />
                                                                    </button>
                                                                    <button 
                                                                        className="btn btn-outline-success"
                                                                        title="Télécharger"
                                                                        onClick={() => this.downloadFile(file.name)}
                                                                    >
                                                                        <FontAwesomeIcon icon={faDownload} />
                                                                    </button>
                                                                </div>
                                                            )}
                                                        </td>
                                                    </tr>
                                                ))}
                                                {folderContents.length === 0 && (
                                                    <tr>
                                                        <td colSpan="5" className="text-center text-muted">
                                                            Aucun fichier trouvé
                                                        </td>
                                                    </tr>
                                                )}
                                            </tbody>
                                        </table>
                                    )}
                                </div>
                                
                                {this.renderPagination()}
                            </div>
                        )}
                        {!selectedFolder && (
                            <div className="card">
                                <div className="card-body text-center text-muted p-5">
                                    <FontAwesomeIcon icon={faFolder} size="3x" className="mb-3" />
                                    <p>Sélectionnez une machine pour voir les logs</p>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        );
    }
}
