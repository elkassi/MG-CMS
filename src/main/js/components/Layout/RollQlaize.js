import React, { Component } from 'react'
import axios from 'axios'
import '../../styles/RollQlaize.scss'
import Select from "react-select";
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import * as XLSX from 'xlsx';

class RollQlaize extends Component {
  constructor(props) {
    super(props)
    this.state = {
      originalData: [],
      cuttingRequestData: [],
      demandeData: [],
      loading: true,
      error: null,
      sortColumn: null,
      sortDirection: 'asc',
      showModal: false,
      selectedDemande: null,
      loadingSeries: [], // Track which series are currently being processed
      showImportModal: false,
      excelData: [],
      excelFile: null,
      importLoading: false,
      bulkUpdate: {
        selectedItemNumber: '',
        laizeValue: ''
      },
      filters: {
        itemNumber: '',
        ref: '',
        location: '',
        fournisseur: ''
      }
    }
  }

  componentDidMount() {
    this.fetchStockData()
  }

  fetchStockData = async () => {
    try {
      this.setState({ loading: true })
      const response = await axios.get('/api/validationQLaize/stockQLaize')

      // Extract unique materials from stock data
      const materials = [...new Set(response.data.map(item => item.itemNumber).filter(Boolean))]
      console.log('Extracted materials for cutting request:', materials)

      // Fetch cutting request data
      let cuttingRequestData = []
      if (materials.length > 0) {
        try {
          console.log('Sending POST request to cutting API with materials:', materials)
          const cuttingResponse = await axios.post('/api/cuttingRequestSerieData/getReportByPartNumberMaterial', materials)
          cuttingRequestData = cuttingResponse.data || []
          console.log('Received cutting request data:', cuttingRequestData)
        } catch (cuttingError) {
          console.error('Error fetching cutting request data:', cuttingError)
          // Continue without cutting data if API fails
        }
      }

      // Extract unique series from cutting request data for fetching demandes
      const seriesArray = cuttingRequestData
        .map(item => item.serie)
        .filter(Boolean)
        .filter(serie => serie !== null && serie !== undefined)
      const series = [...new Set(seriesArray)]
      console.log('Extracted series for demande request:', series)

      // Fetch demande data for series
      let demandeData = []
      if (series.length > 0) {
        try {
          console.log('Sending GET request to demande API with series:', series)
          const seriesParam = Array.isArray(series) ? series.join(',') : String(series)
          console.log('Demande API series parameter:', seriesParam)
          const demandeResponse = await axios.get('/api/demandeChangementSerie/listSeries', {
            params: { series: seriesParam }
          })
          demandeData = demandeResponse.data || []
          console.log('Received demande data:', demandeData)
        } catch (demandeError) {
          console.error('Error fetching demande data:', demandeError)
          // Continue without demande data if API fails
        }
      }

      this.setState({
        originalData: response.data,
        cuttingRequestData: cuttingRequestData,
        demandeData: demandeData,
        loading: false
      })
    } catch (error) {
      console.error('Fatal error in fetchStockData:', error)
      console.error('Error message:', error.message)
      console.error('Error stack:', error.stack)
      this.setState({ error: 'Error loading stock data: ' + (error.message || 'Unknown error'), loading: false })
    }
  }

  fetchDemandeData = async (series) => {
    try {
      if (series && series.length > 0) {
        console.log('Fetching demande data for series:', series)
        const demandeResponse = await axios.get('/api/demandeChangementSerie/listSeries', {
          params: { series: series }
        })
        return demandeResponse.data || []
      }
      return []
    } catch (error) {
      console.error('Error fetching demande data:', error)
      return []
    }
  }


  handleInputChange = (e, index) => {
    const value = e.target.value
    // Regex to allow float numbers with 1-3 digits after dot
    const floatRegex = /^\d*\.?\d{0,3}$/

    if (value === '' || floatRegex.test(value)) {
      // Find the original item to update
      const currentItem = this.getFilteredAndSortedData()[index]

      this.setState(prevState => ({
        originalData: prevState.originalData.map(item =>
          (item.itemNumber === currentItem.itemNumber && item.ref === currentItem.ref)
            ? { ...item, tempLaizeReel: value }
            : item
        )
      }))
    }
  }

  handleSort = (column) => {
    const { sortColumn, sortDirection } = this.state
    let newDirection = 'asc'

    if (sortColumn === column && sortDirection === 'asc') {
      newDirection = 'desc'
    }

    this.setState({
      sortColumn: column,
      sortDirection: newDirection
    })
  }

  handleFilterChange = (column, value) => {
    this.setState(prevState => ({
      filters: {
        ...prevState.filters,
        [column]: value
      }
    }))
  }

  getFilteredAndSortedData = () => {
    const { originalData, filters, sortColumn, sortDirection } = this.state

    if (!originalData) return []

    let filteredData = [...originalData]

    // Apply filters
    // Filter by itemNumber
    if (filters.itemNumber && filters.itemNumber.trim() !== '') {
      filteredData = filteredData.filter(item => {
        const itemValue = item.itemNumber ? item.itemNumber.toString().toLowerCase() : ''
        return itemValue.includes(filters.itemNumber.toLowerCase())
      })
    }

    // Filter by ref
    if (filters.ref && filters.ref.trim() !== '') {
      filteredData = filteredData.filter(item => {
        const itemValue = item.ref ? item.ref.toString().toLowerCase() : ''
        return itemValue.includes(filters.ref.toLowerCase())
      })
    }

    // Filter by location
    if (filters.location && filters.location.trim() !== '') {
      filteredData = filteredData.filter(item => {
        const itemValue = item.location ? item.location.toString().toLowerCase() : ''
        return itemValue.includes(filters.location.toLowerCase())
      })
    }

    // Filter by fournisseur
    if (filters.fournisseur && filters.fournisseur.trim() !== '') {
      filteredData = filteredData.filter(item => {
        const itemValue = item.fournisseur ? item.fournisseur.toString().toLowerCase() : ''
        return itemValue.includes(filters.fournisseur.toLowerCase())
      })
    }

    // Apply sorting
    if (sortColumn) {
      filteredData.sort((a, b) => {
        let aVal = a[sortColumn]
        let bVal = b[sortColumn]

        // Handle null/undefined values
        if (aVal == null) aVal = ''
        if (bVal == null) bVal = ''

        // Handle numeric columns
        if (['qtyOnHand', 'laizeContractuel', 'laizeReel'].includes(sortColumn)) {
          aVal = parseFloat(aVal) || 0
          bVal = parseFloat(bVal) || 0
        } else if (sortColumn === 'validationDate' || sortColumn === 'lastCnt') {
          aVal = aVal ? new Date(aVal) : new Date(0)
          bVal = bVal ? new Date(bVal) : new Date(0)
        } else {
          aVal = aVal.toString().toLowerCase()
          bVal = bVal.toString().toLowerCase()
        }

        if (sortDirection === 'asc') {
          return aVal > bVal ? 1 : (aVal < bVal ? -1 : 0)
        } else {
          return aVal < bVal ? 1 : (aVal > bVal ? -1 : 0)
        }
      })
    }

    return filteredData
  }

  handleKeyPress = async (e, item, index) => {
    if (e.key === 'Enter') {
      await this.saveValidation(item)

      // After saving, find the next empty input and focus on it
      setTimeout(() => {
        const inputs = document.querySelectorAll('.laize-input')
        for (let i = index + 1; i < inputs.length; i++) {
          if (!inputs[i].value || inputs[i].value.trim() === '') {
            inputs[i].focus()
            inputs[i].select()
            break
          }
        }
      }, 100) // Small delay to ensure state is updated
    }
    // Handle arrow key navigation between inputs
    if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
      e.preventDefault()

      const inputs = document.querySelectorAll('.laize-input')
      let targetIndex = -1

      if (e.key === 'ArrowUp' && index > 0) {
        targetIndex = index - 1
      } else if (e.key === 'ArrowDown' && index < inputs.length - 1) {
        targetIndex = index + 1
      }

      if (targetIndex >= 0 && inputs[targetIndex]) {
        inputs[targetIndex].focus()
        inputs[targetIndex].select()
      }
    }
  }

  handleBlur = async (item) => {
    if (item.tempLaizeReel !== undefined && item.tempLaizeReel !== '') {
      await this.saveValidation(item)
    }
  }

  handleWheel = (e) => {
    // Prevent scroll wheel from changing the number input value
    e.target.blur()
  }

  saveValidation = async (item) => {
    try {
      // Ensure the laizeReel value uses dot as decimal separator
      const laizeReelValue = item.tempLaizeReel || item.laizeReel
      const normalizedValue = typeof laizeReelValue === 'string'
        ? laizeReelValue.replace(',', '.')
        : laizeReelValue

      const validationData = {
        ...item,
        laizeReel: parseFloat(normalizedValue)
      }

      const response = await axios.post('/api/validationQLaize', validationData)

      // Update the item in the state with the response data
      this.setState(prevState => ({
        originalData: prevState.originalData.map(stockItem =>
          (stockItem.itemNumber === item.itemNumber && stockItem.ref === item.ref) ? { ...stockItem, ...response.data, tempLaizeReel: undefined } : stockItem
        )
      }))
    } catch (error) {
      console.error('Error saving validation:', error)
      alert('Error saving validation. Please try again.')
    }
  }

  handleBulkUpdateChange = (field, value) => {
    // If it's the laizeValue field, validate the input
    if (field === 'laizeValue') {
      // Regex to allow float numbers with 1-3 digits after dot
      const floatRegex = /^\d*\.?\d{0,3}$/
      if (value !== '' && !floatRegex.test(value)) {
        return // Don't update state if input is invalid
      }
    }

    this.setState(prevState => ({
      bulkUpdate: {
        ...prevState.bulkUpdate,
        [field]: value
      }
    }))
  }

  handleBulkApply = async () => {
    const { bulkUpdate, originalData } = this.state
    const { selectedItemNumber, laizeValue } = bulkUpdate

    if (!selectedItemNumber || !laizeValue) {
      alert('Please select an item number and enter a laize value')
      return
    }

    // Regex to validate float numbers with 1-3 digits after dot
    const floatRegex = /^\d*\.?\d{0,3}$/
    if (!floatRegex.test(laizeValue)) {
      alert('Please enter a valid laize value (numbers with up to 3 decimal places)')
      return
    }

    try {
      // Filter items that match the selected item number and don't have laizeReel
      const itemsToUpdate = originalData.filter(item =>
        item.itemNumber === selectedItemNumber && (!item.laizeReel || item.laizeReel === '')
      )

      if (itemsToUpdate.length === 0) {
        alert('No items found for this item number that need laize reel validation')
        return
      }

      // Confirm with user
      const confirmed = window.confirm(
        `This will apply laize reel value "${laizeValue}" to ${itemsToUpdate.length} rows with item number "${selectedItemNumber}". Continue?`
      )

      if (!confirmed) return

      // Update all matching items
      for (const item of itemsToUpdate) {
        await this.saveValidation({
          ...item,
          tempLaizeReel: laizeValue
        })
      }

      // Clear the bulk update form
      this.setState({
        bulkUpdate: {
          selectedItemNumber: '',
          laizeValue: ''
        }
      })

      alert(`Successfully updated ${itemsToUpdate.length} items`)
    } catch (error) {
      console.error('Error in bulk update:', error)
      alert('Error during bulk update. Please try again.')
    }
  }

  getUniqueItemNumbers = () => {
    const { originalData } = this.state

    // Filter item numbers that have at least one item without laize reel
    const itemNumbersWithoutLaize = [...new Set(
      originalData
        .filter(item => !item.laizeReel || item.laizeReel === '')
        .map(item => item.itemNumber)
    )].sort()

    return itemNumbersWithoutLaize.map(itemNumber => ({
      value: itemNumber,
      label: itemNumber
    }))
  }

  formatDate = (dateString) => {
    if (!dateString) return 'Not validated'
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  }

  generateSummaryData = (stockData) => {
    const { cuttingRequestData, demandeData } = this.state
    const groupedData = {}

    // First, process stock data
    stockData.forEach(item => {
      const laizeReel = item.laizeReel || item.tempLaizeReel || 'Not Validated'
      const key = `${item.itemNumber}_${laizeReel}`

      if (!groupedData[key]) {
        groupedData[key] = {
          itemNumber: item.itemNumber,
          laizeReel: laizeReel,
          totalQty: 0,
          unit: item.um,
          count: 0,
          totalLongueurTotal: 0
        }
      }

      groupedData[key].totalQty += parseFloat(item.qtyOnHand) || 0
      groupedData[key].count += 1
    })

    // Add longueur total from cutting request data
    cuttingRequestData.forEach(cuttingItem => {

      // Check if there's a demande for this serie that changes the laize
      const demande = demandeData.find(d => d.serie === cuttingItem.serie)
      if (!demande || !demande.laize) {
        return;
      }
      const laizeValue = demande.laize
      const partNumberMaterial = cuttingItem.partNumberMaterial
      const key = `${partNumberMaterial}_${laizeValue}`

      // Check if the key exists in groupedData, if not create it
      if (!groupedData[key]) {
        groupedData[key] = {
          itemNumber: partNumberMaterial,
          laizeReel: laizeValue,
          totalQty: 0,
          unit: 'M', // Default unit
          count: 0,
          totalLongueurTotal: 0
        }
      }

      /*
      groupedData[key].totalLongueurTotal += parseFloat(cuttingItem.longueurTotal) || 0
      we need to fill totalLongueurTotal with the serie longueurTotal
      */
      groupedData[key].totalLongueurTotal += parseFloat(cuttingItem.longueurTotal) || 0

    })

    return Object.values(groupedData).sort((a, b) => {
      if (a.itemNumber !== b.itemNumber) {
        return a.itemNumber.localeCompare(b.itemNumber)
      }
      // Sort by laizeReel, but put 'Not Validated' at the end
      if (a.laizeReel === 'Not Validated' && b.laizeReel !== 'Not Validated') return 1
      if (b.laizeReel === 'Not Validated' && a.laizeReel !== 'Not Validated') return -1
      if (a.laizeReel === 'Not Validated' && b.laizeReel === 'Not Validated') return 0
      return parseFloat(a.laizeReel) - parseFloat(b.laizeReel)
    })
  }

  generateCuttingRequestSummary = () => {
    const { cuttingRequestData } = this.state
    const groupedData = {}

    cuttingRequestData.forEach(item => {
      const material = item.partNumberMaterial

      if (!groupedData[material]) {
        groupedData[material] = {
          partNumberMaterial: material,
          totalLongueurTotal: 0,
          count: 0,
          items: []
        }
      }

      groupedData[material].totalLongueurTotal += parseFloat(item.longueurTotal) || 0
      groupedData[material].count += 1
      groupedData[material].items.push(item)
    })

    return Object.values(groupedData).sort((a, b) =>
      a.partNumberMaterial.localeCompare(b.partNumberMaterial)
    )
  }

  handleSendRequest = async (item) => {
    const serie = item.serie;
    if (!item.newLaize) return;

    // Add serie to loading array
    this.setState({
      loadingSeries: [...this.state.loadingSeries, serie]
    });

    try {
      const response = await axios.post('/api/demandeChangementSerie', {
        serie: item.serie,
        laize: item.newLaize,
        typeDemande: "QLaize dérogé",
        // description: "Demande du logitique pour consommation de stock qlaize"
      })
      if (response.status === 200 || response.status === 201) {
        // Add the new demande to the state
        const newDemande = response.data;
        this.setState({
          demandeData: [...this.state.demandeData, newDemande],
          loadingSeries: this.state.loadingSeries.filter(s => s !== serie)
        })

        alert('Demande created successfully!')
      }
    } catch (error) {
      console.error('Error sending request:', error)
      alert('Error creating demande. Please try again.')

      // Remove serie from loading array on error
      this.setState(prevState => ({
        loadingSeries: prevState.loadingSeries.filter(s => s !== serie)
      }));
    }
  }

  handleViewDemandeDetails = (demande) => {
    this.setState({
      selectedDemande: demande,
      showModal: true
    })
  }

  handleCloseModal = () => {
    this.setState({
      showModal: false,
      selectedDemande: null
    })
  }

  // Excel Import Methods
  handleShowImportModal = () => {
    this.setState({
      showImportModal: true,
      excelData: [],
      excelFile: null
    })
  }

  handleCloseImportModal = () => {
    this.setState({
      showImportModal: false,
      excelData: [],
      excelFile: null
    })
  }

  handleFileUpload = (event) => {
    const file = event.target.files[0]
    if (!file) return

    this.setState({ excelFile: file, importLoading: true })

    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const data = new Uint8Array(e.target.result)
        const workbook = XLSX.read(data, { type: 'array' })
        const sheetName = workbook.SheetNames[0]
        const worksheet = workbook.Sheets[sheetName]
        const jsonData = XLSX.utils.sheet_to_json(worksheet)

        // Validate and process the data
        const processedData = jsonData.map((row, index) => {
          // Format ref according to requirements
          let ref = row['ref'] || row['Ref'] || row['Reference'] || row['reference'] || ''
          ref = this.formatRef(ref)
          
          return {
            id: index + 1,
            itemNumber: row['itemNumber'] || row['Item Number'] || row['itemnumber'] || '',
            ref: ref,
            laizeReel: row['laizeReel'] || row['Laize Reel'] || row['laize_reel'] || row['laize reel'] || '',
            isValid: true,
            error: ''
          }
        })

        // Validate each row
        processedData.forEach(row => {
          if (!row.itemNumber || !row.ref || !row.laizeReel) {
            row.isValid = false
            row.error = 'Missing required fields'
          } else if (isNaN(parseFloat(row.laizeReel))) {
            row.isValid = false
            row.error = 'Laize Reel must be a number'
          }
        })

        this.setState({ 
          excelData: processedData,
          importLoading: false
        })
      } catch (error) {
        console.error('Error reading Excel file:', error)
        alert('Error reading Excel file. Please check the file format.')
        this.setState({ importLoading: false })
      }
    }
    reader.readAsArrayBuffer(file)
  }

  // Helper function to format ref according to requirements
  formatRef = (ref) => {
    if (!ref) return ref
    
    let formattedRef = ref.toString()
    
    // If ref starts with 'S', remove it
    if (formattedRef.toUpperCase().startsWith('S')) {
      formattedRef = formattedRef.substring(1)
    }
    
    // Take only the last 8 characters
    if (formattedRef.length > 8) {
      formattedRef = formattedRef.substring(formattedRef.length - 8)
    }
    
    return formattedRef
  }

  handleSaveExcelData = async () => {
    const { excelData } = this.state
    const validData = excelData.filter(row => row.isValid)

    if (validData.length === 0) {
      alert('No valid data to save')
      return
    }

    const confirmed = window.confirm(
      `This will save ${validData.length} rows. Continue?`
    )

    if (!confirmed) return

    this.setState({ importLoading: true })

    try {
      let successCount = 0
      let errorCount = 0

      for (const row of validData) {
        try {
          // Format the ref according to requirements
          const formattedRef = this.formatRef(row.ref)
          
          // Find the corresponding item in originalData
          const existingItem = this.state.originalData.find(
            item => item.itemNumber === row.itemNumber && item.ref === formattedRef
          )

          // Create validation data - use existing item if found, otherwise create new structure
          const validationData = existingItem ? {
            ...existingItem,
            tempLaizeReel: row.laizeReel
          } : {
            itemNumber: row.itemNumber,
            ref: formattedRef,
            laizeReel: parseFloat(row.laizeReel),
            // Add default values for required fields if they don't exist
            qtyOnHand: 0,
            location: '',
            fournisseur: '',
            um: 'M',
            status: 'AVAIL2'
          }

          // Use direct API call instead of saveValidation for new items
          if (existingItem) {
            await this.saveValidation(validationData)
          } else {
            // Direct API call for new items
            const response = await axios.post('/api/validationQLaize', validationData)
            console.log('Saved new item:', response.data)
          }
          
          successCount++
        } catch (error) {
          errorCount++
          console.error(`Error saving row: ${row.itemNumber} - ${row.ref}`, error)
        }
      }

      alert(`Import completed!\nSuccessful: ${successCount}\nErrors: ${errorCount}`)
      
      if (successCount > 0) {
        this.handleCloseImportModal()
        this.fetchStockData() // Refresh data
      }
    } catch (error) {
      console.error('Error during bulk import:', error)
      alert('Error during import. Please try again.')
    } finally {
      this.setState({ importLoading: false })
    }
  }

  handleSaveExcelRow = async (row) => {
    if (!row.isValid) return

    this.setState({ importLoading: true })

    try {
      // Format the ref according to requirements
      const formattedRef = this.formatRef(row.ref)
      
      const existingItem = this.state.originalData.find(
        item => item.itemNumber === row.itemNumber && item.ref === formattedRef
      )

      // Create validation data - use existing item if found, otherwise create new structure
      const validationData = existingItem ? {
        ...existingItem,
        tempLaizeReel: row.laizeReel
      } : {
        itemNumber: row.itemNumber,
        ref: formattedRef,
        laizeReel: parseFloat(row.laizeReel),
        // Add default values for required fields if they don't exist
        qtyOnHand: 0,
        location: '',
        fournisseur: '',
        um: 'MT',
        status: 'AVAIL2'
      }

      // Use appropriate save method
      if (existingItem) {
        await this.saveValidation(validationData)
      } else {
        // Direct API call for new items
        const response = await axios.post('/api/validationQLaize', validationData)
        console.log('Saved new item:', response.data)
      }
      
      // Update the row status
      const updatedExcelData = this.state.excelData.map(r => 
        r.id === row.id ? { ...r, saved: true } : r
      )
      
      this.setState({ excelData: updatedExcelData })
      alert('Row saved successfully!')
    } catch (error) {
      console.error('Error saving row:', error)
      alert('Error saving row. Please try again.')
    } finally {
      this.setState({ importLoading: false })
    }
  }

  formatDateForDisplay = (dateString) => {
    if (!dateString) return '-'
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  renderDemandeModal = () => {
    const { showModal, selectedDemande } = this.state

    if (!showModal || !selectedDemande) return null

    return (
      <div
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }}
        onClick={this.handleCloseModal}
      >
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: '8px',
            padding: '0',
            maxWidth: '700px',
            width: '90%',
            maxHeight: '75vh',
            overflow: 'hidden',
            boxShadow: '0 10px 25px rgba(0, 0, 0, 0.15)',
            border: '1px solid #e0e0e0'
          }}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Modal Header */}
          <div style={{
            background: 'linear-gradient(135deg, #EE3124 0%, #c8251a 100%)',
            color: '#ffffff',
            padding: '12px 20px',
            borderRadius: '8px 8px 0 0',
            position: 'relative'
          }}>
            <h2 style={{
              margin: 0,
              fontSize: '1.2rem',
              fontWeight: '600'
            }}>
              Demande Changement Serie
            </h2>
            <p style={{
              margin: '3px 0 0 0',
              fontSize: '0.8rem',
              opacity: 0.9
            }}>
              ID: {selectedDemande.id} | Statut: {selectedDemande.statut}
            </p>
            <button
              onClick={this.handleCloseModal}
              style={{
                position: 'absolute',
                top: '10px',
                right: '15px',
                background: 'transparent',
                border: 'none',
                color: '#ffffff',
                fontSize: '20px',
                cursor: 'pointer',
                padding: '4px',
                borderRadius: '50%',
                width: '30px',
                height: '30px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'background-color 0.2s ease'
              }}
              onMouseEnter={(e) => e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.2)'}
              onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
            >
              ×
            </button>
          </div>

          {/* Modal Body */}
          <div style={{
            padding: '15px',
            maxHeight: 'calc(75vh - 120px)',
            overflowY: 'auto'
          }}>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
              gap: '15px'
            }}>
              {/* Basic Information */}
              <div style={{
                background: '#f8f9fa',
                borderRadius: '6px',
                padding: '12px',
                border: '1px solid #e9ecef'
              }}>
                <h3 style={{
                  margin: '0 0 10px 0',
                  color: '#EE3124',
                  fontSize: '0.95rem',
                  fontWeight: '600',
                  borderBottom: '2px solid #EE3124',
                  paddingBottom: '5px'
                }}>
                  Informations de Base
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ fontSize: '0.8rem' }}><strong>Serie:</strong> <span style={{ color: '#666' }}>{selectedDemande.serie || '-'}</span></div>
                  <div style={{ fontSize: '0.8rem' }}><strong>Laize:</strong> <span style={{ color: '#666' }}>{selectedDemande.laize || '-'}</span></div>
                  <div style={{ fontSize: '0.8rem' }}><strong>Sequence:</strong> <span style={{ color: '#666' }}>{selectedDemande.sequence || '-'}</span></div>
                  <div style={{ fontSize: '0.8rem' }}><strong>Part Number Material:</strong> <span style={{ color: '#666' }}>{selectedDemande.partNumberMaterial || '-'}</span></div>
                  <div style={{ fontSize: '0.8rem' }}><strong>Part Numbers:</strong> <span style={{ color: '#666', wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}>{selectedDemande.partNumbers || '-'}</span></div>
                  <div style={{ fontSize: '0.8rem' }}><strong>Placement:</strong> <span style={{ color: '#666' }}>{selectedDemande.placement || '-'}</span></div>
                  <div style={{ fontSize: '0.8rem' }}><strong>New Placement:</strong> <span style={{ color: '#666' }}>{selectedDemande.newPlacement || '-'}</span></div>
                </div>
              </div>

              {/* Creation Information */}
              <div style={{
                background: '#f8f9fa',
                borderRadius: '8px',
                padding: '20px',
                border: '1px solid #e9ecef'
              }}>
                <h3 style={{
                  margin: '0 0 15px 0',
                  color: '#EE3124',
                  fontSize: '1.1rem',
                  fontWeight: '600',
                  borderBottom: '2px solid #EE3124',
                  paddingBottom: '8px'
                }}>
                  Informations de Création
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <div><strong>Créé par:</strong> <span style={{ color: '#666' }}>{selectedDemande.creePar || '-'}</span></div>
                  <div><strong>Date de Création:</strong> <span style={{ color: '#666' }}>{this.formatDateForDisplay(selectedDemande.dateCreation)}</span></div>
                  <div><strong>Statut:</strong>
                    <span style={{
                      color: '#ffffff',
                      backgroundColor: selectedDemande.statut.startsWith('En attente') ? '#ffc107' :
                        selectedDemande.statut === 'Traitée' ? '#28a745' :
                          selectedDemande.statut === 'Refusée' ? '#dc3545' : '#6c757d',
                      padding: '3px 8px',
                      borderRadius: '12px',
                      fontSize: '0.8rem',
                      marginLeft: '8px'
                    }}>
                      {selectedDemande.statut || '-'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Process Response */}
              <div style={{
                background: '#f8f9fa',
                borderRadius: '8px',
                padding: '20px',
                border: '1px solid #e9ecef'
              }}>
                <h3 style={{
                  margin: '0 0 15px 0',
                  color: '#EE3124',
                  fontSize: '1.1rem',
                  fontWeight: '600',
                  borderBottom: '2px solid #EE3124',
                  paddingBottom: '8px'
                }}>
                  Réponse {selectedDemande.departementValidation}
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <div><strong>Réponse:</strong> <span style={{ color: '#666' }}>{selectedDemande.reponseDepartement || '-'}</span></div>
                  <div><strong>Confirmé par:</strong> <span style={{ color: '#666' }}>{selectedDemande.confirmeParDepartement || '-'}</span></div>
                  <div><strong>Date Confirmation:</strong> <span style={{ color: '#666' }}>{this.formatDateForDisplay(selectedDemande.dateConfirmationDepartement)}</span></div>
                </div>
              </div>

              {/* CAD Response */}
              <div style={{
                background: '#f8f9fa',
                borderRadius: '8px',
                padding: '20px',
                border: '1px solid #e9ecef'
              }}>
                <h3 style={{
                  margin: '0 0 15px 0',
                  color: '#EE3124',
                  fontSize: '1.1rem',
                  fontWeight: '600',
                  borderBottom: '2px solid #EE3124',
                  paddingBottom: '8px'
                }}>
                  Réponse CAD
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <div><strong>CAD:</strong> <span style={{ color: '#666' }}>{selectedDemande.reponse || '-'}</span></div>
                  <div><strong>Confirmé par:</strong> <span style={{ color: '#666' }}>{selectedDemande.confirmePar || '-'}</span></div>
                  <div><strong>Date Confirmation:</strong> <span style={{ color: '#666' }}>{this.formatDateForDisplay(selectedDemande.dateConfirmation)}</span></div>
                </div>
              </div>
            </div>

            {/* Description and Other Changes */}
            <div style={{
              marginTop: '25px',
              display: 'grid',
              gridTemplateColumns: '1fr',
              gap: '20px'
            }}>
              {selectedDemande.description && (
                <div style={{
                  background: '#f8f9fa',
                  borderRadius: '8px',
                  padding: '20px',
                  border: '1px solid #e9ecef'
                }}>
                  <h3 style={{
                    margin: '0 0 15px 0',
                    color: '#EE3124',
                    fontSize: '1.1rem',
                    fontWeight: '600',
                    borderBottom: '2px solid #EE3124',
                    paddingBottom: '8px'
                  }}>
                    Description
                  </h3>
                  <p style={{
                    color: '#666',
                    lineHeight: '1.6',
                    margin: 0,
                    whiteSpace: 'pre-wrap'
                  }}>
                    {selectedDemande.description}
                  </p>
                </div>
              )}

              {selectedDemande.autreChangement && (
                <div style={{
                  background: '#f8f9fa',
                  borderRadius: '8px',
                  padding: '20px',
                  border: '1px solid #e9ecef'
                }}>
                  <h3 style={{
                    margin: '0 0 15px 0',
                    color: '#EE3124',
                    fontSize: '1.1rem',
                    fontWeight: '600',
                    borderBottom: '2px solid #EE3124',
                    paddingBottom: '8px'
                  }}>
                    Autre Changement
                  </h3>
                  <p style={{
                    color: '#666',
                    lineHeight: '1.6',
                    margin: 0,
                    whiteSpace: 'pre-wrap'
                  }}>
                    {selectedDemande.autreChangement}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Modal Footer */}
          <div style={{
            padding: '10px 15px',
            borderTop: '1px solid #e9ecef',
            background: '#f8f9fa',
            borderRadius: '0 0 8px 8px',
            textAlign: 'right',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <button
              onClick={() => {
                const url = `/demandeChangementSerieValidation?id=${selectedDemande.id}`;
                window.open(url, '_blank');
              }}
              style={{
                background: '#EE3124',
                color: '#ffffff',
                border: 'none',
                padding: '6px 12px',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '0.8rem',
                fontWeight: '500',
                transition: 'background-color 0.2s ease'
              }}
              onMouseEnter={(e) => e.target.style.backgroundColor = '#c8251a'}
              onMouseLeave={(e) => e.target.style.backgroundColor = '#EE3124'}
            >
              Voir Validation
            </button>
            <button
              onClick={this.handleCloseModal}
              style={{
                background: '#6c757d',
                color: '#ffffff',
                border: 'none',
                padding: '6px 12px',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '0.8rem',
                fontWeight: '500',
                transition: 'background-color 0.2s ease'
              }}
              onMouseEnter={(e) => e.target.style.backgroundColor = '#5a6268'}
              onMouseLeave={(e) => e.target.style.backgroundColor = '#6c757d'}
            >
              Fermer
            </button>
          </div>
        </div>
      </div>
    )
  }

  renderImportModal = () => {
    const { showImportModal, excelData, importLoading } = this.state

    if (!showImportModal) return null

    return (
      <div
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }}
        onClick={this.handleCloseImportModal}
      >
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: '8px',
            padding: '0',
            maxWidth: '90%',
            width: '800px',
            maxHeight: '85vh',
            overflow: 'hidden',
            boxShadow: '0 10px 25px rgba(0, 0, 0, 0.15)',
            border: '1px solid #e0e0e0'
          }}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div style={{
            background: 'linear-gradient(135deg, #17a2b8 0%, #138496 100%)',
            color: '#ffffff',
            padding: '15px 20px',
            borderRadius: '8px 8px 0 0',
            position: 'relative'
          }}>
            <h2 style={{
              margin: 0,
              fontSize: '1.2rem',
              fontWeight: '600'
            }}>
              Import Excel File
            </h2>
            <p style={{
              margin: '5px 0 0 0',
              fontSize: '0.85rem',
              opacity: 0.9
            }}>
              Upload Excel file with columns: itemNumber, ref, laizeReel
            </p>
            <button
              onClick={this.handleCloseImportModal}
              style={{
                position: 'absolute',
                top: '10px',
                right: '15px',
                background: 'transparent',
                border: 'none',
                color: '#ffffff',
                fontSize: '20px',
                cursor: 'pointer',
                padding: '4px',
                borderRadius: '50%',
                width: '30px',
                height: '30px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'background-color 0.2s ease'
              }}
              onMouseEnter={(e) => e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.2)'}
              onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
            >
              ×
            </button>
          </div>

          {/* Content */}
          <div style={{
            padding: '20px',
            maxHeight: 'calc(85vh - 140px)',
            overflowY: 'auto'
          }}>
            {/* File Upload Section */}
            <div style={{
              border: '2px dashed #17a2b8',
              borderRadius: '8px',
              padding: '20px',
              textAlign: 'center',
              marginBottom: '20px',
              backgroundColor: '#f8f9fa'
            }}>
              <div style={{ marginBottom: '10px' }}>
                <i style={{ fontSize: '3rem', color: '#17a2b8' }}>📄</i>
              </div>
              <p style={{ margin: '10px 0', fontSize: '0.9rem', color: '#6c757d' }}>
                Select an Excel file (.xlsx, .xls) with the following columns:
              </p>
              <ul style={{ 
                textAlign: 'left', 
                display: 'inline-block', 
                margin: '10px 0',
                fontSize: '0.85rem',
                color: '#6c757d'
              }}>
                <li><strong>itemNumber</strong> - Item Number</li>
                <li><strong>ref</strong> - Reference/Serial (max 8 chars, 'S' prefix removed)</li>
                <li><strong>laizeReel</strong> - Laize Reel Value (numeric)</li>
              </ul>
              <div style={{
                background: '#e7f3ff',
                border: '1px solid #bee5eb',
                borderRadius: '4px',
                padding: '10px',
                margin: '10px 0',
                fontSize: '0.8rem',
                color: '#0c5460'
              }}>
                <strong>Note:</strong> Items will be saved even if they don't exist in current stock. 
                References starting with 'S' will have it removed, and only the last 8 characters will be kept.
              </div>
              <div style={{ marginTop: '15px' }}>
                <input
                  type="file"
                  accept=".xlsx,.xls"
                  onChange={this.handleFileUpload}
                  style={{ display: 'none' }}
                  id="excel-file-input"
                />
                <label
                  htmlFor="excel-file-input"
                  style={{
                    background: '#17a2b8',
                    color: '#ffffff',
                    padding: '10px 20px',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    display: 'inline-block',
                    fontSize: '0.9rem',
                    fontWeight: '500',
                    transition: 'background-color 0.2s ease'
                  }}
                  onMouseEnter={(e) => e.target.style.backgroundColor = '#138496'}
                  onMouseLeave={(e) => e.target.style.backgroundColor = '#17a2b8'}
                >
                  Choose Excel File
                </label>
              </div>
            </div>

            {/* Loading Indicator */}
            {importLoading && (
              <div style={{
                textAlign: 'center',
                padding: '20px',
                fontSize: '0.9rem',
                color: '#6c757d'
              }}>
                <div style={{
                  width: '30px',
                  height: '30px',
                  border: '3px solid #f3f3f3',
                  borderTop: '3px solid #17a2b8',
                  borderRadius: '50%',
                  animation: 'spin 1s linear infinite',
                  margin: '0 auto 10px'
                }}></div>
                Processing...
              </div>
            )}

            {/* Excel Data Table */}
            {excelData.length > 0 && !importLoading && (
              <div>
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '15px'
                }}>
                  <h3 style={{
                    margin: 0,
                    color: '#17a2b8',
                    fontSize: '1.1rem'
                  }}>
                    Excel Data Preview ({excelData.length} rows)
                  </h3>
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button
                      onClick={this.handleSaveExcelData}
                      disabled={importLoading || excelData.filter(row => row.isValid).length === 0}
                      style={{
                        background: '#28a745',
                        color: '#ffffff',
                        border: 'none',
                        padding: '8px 16px',
                        borderRadius: '4px',
                        fontSize: '0.85rem',
                        fontWeight: '500',
                        cursor: importLoading ? 'not-allowed' : 'pointer',
                        opacity: importLoading || excelData.filter(row => row.isValid).length === 0 ? 0.6 : 1
                      }}
                    >
                      Save All Valid Rows
                    </button>
                  </div>
                </div>

                <div style={{
                  border: '1px solid #dee2e6',
                  borderRadius: '4px',
                  overflow: 'hidden'
                }}>
                  <table style={{
                    width: '100%',
                    borderCollapse: 'collapse',
                    fontSize: '0.8rem'
                  }}>
                    <thead style={{
                      backgroundColor: '#17a2b8',
                      color: '#ffffff'
                    }}>
                      <tr>
                        <th style={{ padding: '10px', textAlign: 'left' }}>Item Number</th>
                        <th style={{ padding: '10px', textAlign: 'left' }}>Ref</th>
                        <th style={{ padding: '10px', textAlign: 'left' }}>Laize Reel</th>
                        <th style={{ padding: '10px', textAlign: 'center' }}>Status</th>
                        <th style={{ padding: '10px', textAlign: 'center' }}>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {excelData.map((row, index) => {
                        // Check if item exists in current stock
                        const existsInStock = this.state.originalData.some(
                          item => item.itemNumber === row.itemNumber && item.ref === row.ref
                        )
                        
                        return (
                          <tr key={index} style={{
                            backgroundColor: row.isValid ? (row.saved ? '#d4edda' : '#fff') : '#f8d7da',
                            borderBottom: '1px solid #dee2e6'
                          }}>
                            <td style={{ padding: '8px' }}>{row.itemNumber}</td>
                            <td style={{ padding: '8px' }}>
                              {row.ref}
                              {!existsInStock && row.isValid && (
                                <span style={{
                                  marginLeft: '5px',
                                  fontSize: '0.7rem',
                                  color: '#17a2b8',
                                  fontWeight: '600'
                                }}>
                                  (New)
                                </span>
                              )}
                            </td>
                            <td style={{ padding: '8px' }}>{row.laizeReel}</td>
                            <td style={{ padding: '8px', textAlign: 'center' }}>
                              {row.saved ? (
                                <span style={{
                                  color: '#155724',
                                  fontWeight: '600'
                                }}>
                                  ✓ Saved
                                </span>
                              ) : row.isValid ? (
                                <span style={{
                                  color: '#28a745',
                                  fontWeight: '600'
                                }}>
                                  ✓ Valid
                                </span>
                              ) : (
                                <span style={{
                                  color: '#721c24',
                                  fontWeight: '600'
                                }}>
                                  ✗ {row.error}
                                </span>
                              )}
                            </td>
                            <td style={{ padding: '8px', textAlign: 'center' }}>
                              {row.isValid && !row.saved && (
                                <button
                                  onClick={() => this.handleSaveExcelRow(row)}
                                  disabled={importLoading}
                                  style={{
                                    background: '#007bff',
                                    color: '#ffffff',
                                    border: 'none',
                                    padding: '4px 8px',
                                    borderRadius: '3px',
                                    fontSize: '0.75rem',
                                    cursor: importLoading ? 'not-allowed' : 'pointer',
                                    opacity: importLoading ? 0.6 : 1
                                  }}
                                >
                                  Save
                                </button>
                              )}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>

          {/* Footer */}
          <div style={{
            padding: '15px 20px',
            borderTop: '1px solid #e9ecef',
            background: '#f8f9fa',
            borderRadius: '0 0 8px 8px',
            textAlign: 'right'
          }}>
            <button
              onClick={this.handleCloseImportModal}
              style={{
                background: '#6c757d',
                color: '#ffffff',
                border: 'none',
                padding: '8px 16px',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '0.85rem',
                fontWeight: '500',
                transition: 'background-color 0.2s ease'
              }}
              onMouseEnter={(e) => e.target.style.backgroundColor = '#5a6268'}
              onMouseLeave={(e) => e.target.style.backgroundColor = '#6c757d'}
            >
              Close
            </button>
          </div>
        </div>
      </div>
    )
  }


  render() {
    let { loading, error, sortColumn, sortDirection, filters, demandeData, loadingSeries } = this.state
    let stockData = this.getFilteredAndSortedData()
    let summaryData = this.generateSummaryData(stockData)
    let cuttingRequestSummary = this.generateCuttingRequestSummary()

    console.log('Render - cuttingRequestData:', this.state.cuttingRequestData)
    console.log('Render - cuttingRequestSummary:', cuttingRequestSummary)
    console.log('Render - demandeData:', demandeData)
    const { user } = this.props.security;

    if (loading) {
      return (
        <div className="roll-qlaize-container">
          <div className="loading">
            <div className="loading-spinner"></div>
            <p>Loading stock data...</p>
          </div>
        </div>
      )
    }

    if (error) {
      return (
        <div className="roll-qlaize-container">
          <div className="error-message">
            <p>{error}</p>
            <button onClick={this.fetchStockData} className="retry-button">
              Retry
            </button>
          </div>
        </div>
      )
    }




    return (
      <div className="roll-qlaize-container">
        <div className="header">
          <h1>Roll Qlaize Stock Validation</h1>
          <p>Validate the real width (Laize Réel) for each roll item</p>
          <div style={{ display: 'flex', alignItems: 'center', gap: '15px', marginTop: '10px', flexWrap: 'wrap', justifyContent: "center" }}>
            {/* Bulk Update Controls */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', background: '#f8f9fa', padding: '8px 12px', borderRadius: '6px', border: '1px solid #e9ecef' }}>
              <label style={{ fontSize: '0.85rem', fontWeight: '500', color: '#495057' }}>Bulk Update:</label>
              <Select
                classNamePrefix="rs"
                placeholder="Item Number"
                options={this.getUniqueItemNumbers()}
                value={this.state.bulkUpdate.selectedItemNumber ? {
                  value: this.state.bulkUpdate.selectedItemNumber,
                  label: this.state.bulkUpdate.selectedItemNumber
                } : null}
                onChange={(selectedOption) => this.handleBulkUpdateChange('selectedItemNumber', selectedOption ? selectedOption.value : '')}
                styles={{
                  control: (base) => ({
                    ...base,
                    minWidth: '150px',
                    fontSize: '0.8rem',
                    border: '1px solid #ced4da',
                    boxShadow: 'none',
                    '&:hover': {
                      border: '1px solid #adb5bd'
                    }
                  }),
                  menu: (base) => ({
                    ...base,
                    fontSize: '0.8rem',
                    zIndex: 9999
                  })
                }}
                menuPortalTarget={document.body}
                menuPosition="fixed"
              />
              <input
                type="text"
                placeholder="Laize Value"
                value={this.state.bulkUpdate.laizeValue}
                onChange={(e) => this.handleBulkUpdateChange('laizeValue', e.target.value)}
                style={{
                  width: '100px',
                  padding: '4px 8px',
                  border: '1px solid #ced4da',
                  borderRadius: '4px',
                  fontSize: '0.8rem'
                }}
              />
              <button
                onClick={this.handleBulkApply}
                style={{
                  background: '#28a745',
                  color: '#ffffff',
                  border: 'none',
                  padding: '6px 12px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  fontWeight: '500',
                  cursor: 'pointer',
                  transition: 'background-color 0.2s ease'
                }}
                onMouseEnter={(e) => e.target.style.backgroundColor = '#218838'}
                onMouseLeave={(e) => e.target.style.backgroundColor = '#28a745'}
              >
                Apply
              </button>
              {this.state.bulkUpdate.selectedItemNumber && (() => {
                const firstRow = this.state.originalData.find(item => item.itemNumber === this.state.bulkUpdate.selectedItemNumber);
                const totalRows = this.state.originalData.filter(item =>
                  item.itemNumber === this.state.bulkUpdate.selectedItemNumber &&
                  (!item.laizeReel || item.laizeReel === '')
                ).length;
                return (
                  <span style={{ fontSize: '0.75rem', color: '#6c757d', marginLeft: '8px' }}>
                    Laize Contractuel: {firstRow ? firstRow.laizeContractuel : '-'} | Rows to update: {totalRows}
                  </span>
                );
              })()}

            </div>

            <button 
              onClick={this.handleShowImportModal}
              style={{
                background: '#17a2b8',
                color: '#ffffff',
                border: 'none',
                padding: '6px 12px',
                borderRadius: '4px',
                fontSize: '0.85rem',
                fontWeight: '500',
                cursor: 'pointer',
                transition: 'background-color 0.2s ease'
              }}
              onMouseEnter={(e) => e.target.style.backgroundColor = '#138496'}
              onMouseLeave={(e) => e.target.style.backgroundColor = '#17a2b8'}
            >
              Import Excel
            </button>

            <button onClick={this.fetchStockData} className="refresh-button">
              Refresh Data
            </button>
          </div>
        </div>

        <div className="table-container">
          <table className="stock-table">
            <thead>
              <tr>
                <th onClick={() => this.handleSort('itemNumber')} className="sortable">
                  Item Number {sortColumn === 'itemNumber' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => this.handleSort('ref')} className="sortable">
                  Serial {sortColumn === 'ref' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => this.handleSort('location')} className="sortable">
                  Location {sortColumn === 'location' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => this.handleSort('qtyOnHand')} className="sortable">
                  Qty on Hand {sortColumn === 'qtyOnHand' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => this.handleSort('fournisseur')} className="sortable">
                  Fournisseur {sortColumn === 'fournisseur' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => this.handleSort('laizeContractuel')} className="sortable">
                  Laize Contractuel {sortColumn === 'laizeContractuel' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => this.handleSort('laizeReel')} className="sortable">
                  Laize Réel {sortColumn === 'laizeReel' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => this.handleSort('validationDate')} className="sortable">
                  Validation Date {sortColumn === 'validationDate' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
                <th onClick={() => this.handleSort('validatedBy')} className="sortable">
                  Validated By {sortColumn === 'validatedBy' && (sortDirection === 'asc' ? '↑' : '↓')}
                </th>
              </tr>
              <tr className="filter-row">
                <th>
                  <input
                    type="text"
                    placeholder="Filter Item Number"
                    value={filters.itemNumber}
                    onChange={(e) => this.handleFilterChange('itemNumber', e.target.value)}
                    className="filter-input"
                  />
                </th>
                <th>
                  <input
                    type="text"
                    placeholder="Filter Reference"
                    value={filters.ref}
                    onChange={(e) => this.handleFilterChange('ref', e.target.value)}
                    className="filter-input"
                  />
                </th>
                <th>
                  <input
                    type="text"
                    placeholder="Filter Location"
                    value={filters.location}
                    onChange={(e) => this.handleFilterChange('location', e.target.value)}
                    className="filter-input"
                  />
                </th>
                <th></th> {/* No filter for Qty on Hand */}
                <th>
                  <input
                    type="text"
                    placeholder="Filter Fournisseur"
                    value={filters.fournisseur}
                    onChange={(e) => this.handleFilterChange('fournisseur', e.target.value)}
                    className="filter-input"
                  />
                </th>
                <th></th> {/* No filter for Laize Contractuel */}
                <th></th> {/* No filter for Laize Réel */}
                <th></th> {/* No filter for Validation Date */}
                <th></th> {/* No filter for Validated By */}
              </tr>
            </thead>
            <tbody>
              {stockData.map((item, index) => (
                <tr
                  key={index}
                  className={item.validationDate ? 'validated' : 'pending'}>
                  <td className="item-number">{item.itemNumber}</td>
                  <td>{item.ref}</td>
                  <td>{item.location}</td>
                  <td className="quantity">{item.qtyOnHand}</td>
                  {/* {item.um} */}
                  <td>{item.fournisseur}</td>
                  <td className="laize-contractuel">{item.laizeContractuel}</td>
                  <td className="laize-reel-cell">
                    <div className="input-container">
                      {(user && user.roles &&
                        (
                          user.roles.some(role => ["ROLE_VALID_QN_FOURNISSEUR"].includes(role.authority))
                        )) ?
                        <input
                          type="text"
                          value={item.tempLaizeReel !== undefined ? item.tempLaizeReel : (item.laizeReel || '')}
                          onChange={(e) => this.handleInputChange(e, index)}
                          onKeyUp={(e) => this.handleKeyPress(e, item, index)}
                          onBlur={() => this.handleBlur(item)}
                          onWheel={this.handleWheel}
                          className="laize-input"
                          placeholder="Enter real width"
                        />
                        : <span>{item.laizeReel || '-'}</span>
                      }
                    </div>
                  </td>
                  <td>{this.formatDate(item.validationDate)}</td>
                  <td>{item.validatedBy || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Serie Rapport Table */}
        <div className="serie-rapport-section">
          <h2 style={{ color: '#EE3124', borderBottom: '2px solid #EE3124', paddingBottom: '5px' }}>
            Serie Rapport
          </h2>
          <div className="table-container">
            <table
              className="serie-rapport-table"
              style={{
                width: '100%',
                borderCollapse: 'collapse',
                background: '#ffffff',
                borderRadius: '4px',
                overflow: 'hidden',
                boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                border: '1px solid #ddd',
                marginBottom: '10px'
              }}
            >
              <thead style={{ background: '#EE3124', color: '#ffffff' }}>
                <tr>
                  <th style={{ padding: '6px 4px', textAlign: 'left', fontWeight: '600', fontSize: '0.7rem' }}>Serie</th>
                  <th style={{ padding: '6px 4px', textAlign: 'left', fontWeight: '600', fontSize: '0.7rem' }}>Placement</th>
                  <th style={{ padding: '6px 4px', textAlign: 'left', fontWeight: '600', fontSize: '0.7rem' }}>Part Number Material</th>
                  <th style={{ padding: '6px 4px', textAlign: 'right', fontWeight: '600', fontSize: '0.7rem' }}>Longueur</th>
                  <th style={{ padding: '6px 4px', textAlign: 'right', fontWeight: '600', fontSize: '0.7rem' }}>Laize</th>
                  <th style={{ padding: '6px 4px', textAlign: 'center', fontWeight: '600', fontSize: '0.7rem' }}>Nbr Couche</th>
                  <th style={{ padding: '6px 4px', textAlign: 'right', fontWeight: '600', fontSize: '0.7rem' }}>Perimetre</th>
                  <th style={{ padding: '6px 4px', textAlign: 'right', fontWeight: '600', fontSize: '0.7rem' }}>Longueur Total</th>
                  <th style={{ padding: '6px 4px', textAlign: 'right', fontWeight: '600', fontSize: '0.7rem' }}>CM/TC</th>
                  <th style={{ padding: '6px 4px', textAlign: 'center', fontWeight: '600', fontSize: '0.7rem', width: '100px' }}>New Laize</th>
                  <th style={{ padding: '6px 4px', textAlign: 'center', fontWeight: '600', fontSize: '0.7rem' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {this.state.cuttingRequestData.length > 0 ? (
                  this.state.cuttingRequestData.map((item, index) => {
                    const existingDemande = demandeData.find(demande => demande.serie === item.serie)

                    return <tr
                      key={`serie_${index}`}
                      style={{
                        borderBottom: '1px solid #e0e0e0',
                        '&:hover': { backgroundColor: '#f8f9fa' }
                      }}
                    >
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem' }}>{item.serie || '-'}</td>
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem' }}>{item.placement || '-'}</td>
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem', fontWeight: '500' }}>
                        {item.partNumberMaterial || '-'}
                      </td>
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem', textAlign: 'right' }}>
                        {item.longueur ? item.longueur.toFixed(2) : '-'}
                      </td>
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem', textAlign: 'right' }}>
                        {item.laize ? item.laize.toFixed(2) : '-'}
                      </td>
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem', textAlign: 'center' }}>
                        {item.nbrCouche || '-'}
                      </td>
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem', textAlign: 'right' }}>
                        {item.perimetre ? item.perimetre.toFixed(2) : '-'}
                      </td>
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem', textAlign: 'right', fontWeight: '500' }}>
                        {item.longueurTotal ? item.longueurTotal.toFixed(2) : '-'}
                      </td>
                      <td style={{ padding: '6px 4px', fontSize: '0.7rem', textAlign: 'right' }}>
                        {item.indicateur ? item.indicateur.toFixed(2) : '-'}
                      </td>
                      <td style={{ padding: '6px 4px', textAlign: 'center' }}>
                        {existingDemande
                          ? <span style={{ color: '#28a745', fontWeight: 'bold' }}>{existingDemande.laize ? existingDemande.laize.toFixed(2) : '-'}</span>
                          : <Select classNamePrefix="rs" className='col-12 p-0'
                            placeholder="Laize"
                            options={
                              //the option we need to get the reel laize from the stock data
                              summaryData
                                .filter(stock => stock.itemNumber === item.partNumberMaterial && stock.laizeReel !== 'Not Validated')
                                .sort((a, b) => parseFloat(a.laizeReel) - parseFloat(b.laizeReel))
                                .map(stock => ({
                                  value: stock.laizeReel,
                                  label: parseFloat(stock.laizeReel).toFixed(2) + ' (' + (stock.totalQty - (stock.totalLongueurTotal || 0)).toFixed(2) + ')'
                                }))
                            }
                            onChange={(selectedOption) => {
                              const newLaize = selectedOption ? parseFloat(selectedOption.value) : null
                              this.setState(prevState => ({
                                cuttingRequestData: prevState.cuttingRequestData.map(req =>
                                  req.serie === item.serie ? { ...req, newLaize } : req
                                )
                              }))
                            }
                            }
                            value={item.newLaize ? { value: item.newLaize, label: item.newLaize.toFixed(2) } : null}
                            styles={{
                              control: (base) => ({
                                ...base,
                                border: '1px solid #ccc',
                                boxShadow: 'none',
                                '&:hover': {
                                  border: '1px solid #aaa'
                                }
                              }),
                              menuPortal: (base) => ({
                                ...base,
                                zIndex: 9999
                              }),
                              menu: (base) => ({
                                ...base,
                                zIndex: 9999
                              })
                            }}
                            menuPortalTarget={document.body}
                            menuPosition={'fixed'}
                            disabled={!!existingDemande || loadingSeries.includes(item.serie)}
                          />}
                      </td>
                      <td style={{ padding: '6px 4px', textAlign: 'center' }}>
                        {(() => {

                          if (existingDemande) {
                            return (
                              <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                                <div style={{ fontSize: '0.6rem', color: '#666' }}>
                                  ID: {existingDemande.id} | Status: {existingDemande.statut}
                                </div>
                                <button
                                  onClick={() => this.handleViewDemandeDetails(existingDemande)}
                                  style={{
                                    background: '#28a745',
                                    color: '#ffffff',
                                    border: 'none',
                                    padding: '4px 6px',
                                    borderRadius: '2px',
                                    cursor: 'pointer',
                                    fontSize: '0.6rem'
                                  }}
                                >
                                  Voir détails
                                </button>
                              </div>
                            )
                          } else if (item.newLaize) {
                            const isLoading = loadingSeries.includes(item.serie)
                            return (
                              <button
                                onClick={() => this.handleSendRequest(item)}
                                disabled={isLoading}
                                style={{
                                  background: isLoading ? '#ccc' : '#EE3124',
                                  color: isLoading ? '#666' : '#ffffff',
                                  border: 'none',
                                  padding: '4px 8px',
                                  borderRadius: '2px',
                                  cursor: isLoading ? 'not-allowed' : 'pointer',
                                  opacity: isLoading ? 0.7 : 1,
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  gap: '3px',
                                  fontSize: '0.6rem'
                                }}
                              >
                                {isLoading && (
                                  <div style={{
                                    width: '8px',
                                    height: '8px',
                                    border: '1px solid #666',
                                    borderTop: '1px solid transparent',
                                    borderRadius: '50%',
                                    animation: 'spin 1s linear infinite'
                                  }}></div>
                                )}
                                {isLoading ? 'Envoi...' : 'Envoyer'}
                              </button>
                            )
                          }
                        })()}
                      </td>
                    </tr>
                  })
                ) : (
                  <tr>
                    <td
                      colSpan="10"
                      style={{
                        textAlign: 'center',
                        padding: '20px',
                        color: '#999',
                        fontStyle: 'italic'
                      }}
                    >
                      No serie rapport data available
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Cutting Request Summary Table */}
        <div className="cutting-request-section">
          <h2 style={{ color: '#EE3124', borderBottom: '2px solid #EE3124', paddingBottom: '10px' }}>
            Cutting Request Summary by Material
          </h2>
          <div className="table-container">
            <table
              className="cutting-request-table"
              style={{
                width: '100%',
                borderCollapse: 'collapse',
                background: '#ffffff',
                borderRadius: '8px',
                overflow: 'hidden',
                boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
                border: '1px solid #ddd'
              }}
            >
              <thead style={{ background: '#EE3124', color: '#ffffff' }}>
                <tr>
                  <th style={{ padding: '15px', textAlign: 'left', fontWeight: '600' }}>Part Number Material</th>
                  <th style={{ padding: '15px', textAlign: 'left', fontWeight: '600' }}>Total Longueur Total</th>
                  <th style={{ padding: '15px', textAlign: 'left', fontWeight: '600' }}>Number of Records</th>
                  <th style={{ padding: '15px', textAlign: 'left', fontWeight: '600' }}>Comparison</th>
                </tr>
              </thead>
              <tbody>
                {cuttingRequestSummary.length > 0 ? (
                  cuttingRequestSummary.map((item, index) => {
                    // Find corresponding stock data for comparison
                    const stockItem = summaryData.find(stock => stock.itemNumber === item.partNumberMaterial)
                    const stockQty = stockItem ? stockItem.totalQty : 0
                    const difference = stockQty - item.totalLongueurTotal
                    const comparisonClass = difference >= 0 ? 'positive' : 'negative'

                    return (
                      <tr
                        key={`cutting_${item.partNumberMaterial}_${index}`}
                        style={{ borderBottom: '1px solid #e0e0e0' }}
                      >
                        <td style={{ padding: '12px 15px', fontWeight: '600', color: '#000' }} className="material-number">
                          {item.partNumberMaterial}
                        </td>
                        <td style={{ padding: '12px 15px', textAlign: 'right', fontWeight: '500' }} className="longueur-total">
                          {item.totalLongueurTotal.toFixed(2)}
                        </td>
                        <td style={{ padding: '12px 15px', textAlign: 'center', fontWeight: '500' }} className="count">
                          {item.count}
                        </td>
                        <td style={{ padding: '12px 15px' }} className={`comparison ${comparisonClass}`}>
                          <div className="comparison-details" style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <div style={{ fontSize: '0.85rem', padding: '2px 0' }}>Stock: {stockQty.toFixed(2)}</div>
                            <div style={{ fontSize: '0.85rem', padding: '2px 0' }}>Required: {item.totalLongueurTotal.toFixed(2)}</div>
                            <div
                              className={`difference ${comparisonClass}`}
                              style={{
                                fontWeight: '600',
                                padding: '4px 8px',
                                borderRadius: '4px',
                                backgroundColor: difference >= 0 ? '#90EE90' : '#FFB6C1',
                                color: difference >= 0 ? '#006400' : '#8B0000',
                                fontSize: '0.85rem'
                              }}
                            >
                              Difference: {difference.toFixed(2)} {difference >= 0 ? '(Sufficient)' : '(Shortage)'}
                            </div>
                          </div>
                        </td>
                      </tr>
                    )
                  })
                ) : (
                  <tr>
                    <td
                      colSpan="4"
                      className="empty-cutting-data"
                      style={{
                        textAlign: 'center',
                        padding: '20px',
                        color: '#999',
                        fontStyle: 'italic'
                      }}
                    >
                      No cutting request data available
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Summary Table */}
        <div className="summary-section">
          <h2>Summary by Item Number and Laize Réel</h2>
          <div className="table-container">
            <table className="summary-table">
              <thead>
                <tr>
                  <th>Item Number</th>
                  <th>Laize Réel</th>
                  <th>Total Quantity</th>
                  <th>Number of Rolls</th>
                  <th>Total Longueur Required</th>
                  <th>Difference</th>
                </tr>
              </thead>
              <tbody>
                {summaryData.map((item, index) => {
                  const difference = item.totalQty - item.totalLongueurTotal
                  const isPositive = difference >= 0

                  return (
                    <tr key={`${item.itemNumber}_${item.laizeReel}_${index}`} className={item.laizeReel === 'Not Validated' ? 'not-validated-summary' : 'validated-summary'}>
                      <td className="item-number">{item.itemNumber}</td>
                      <td className={`laize-value ${item.laizeReel === 'Not Validated' ? 'not-validated' : 'validated'}`}>
                        {item.laizeReel}
                      </td>
                      <td className="quantity">
                        {item.totalQty.toFixed(2)}
                      </td>
                      <td className="count">{item.count}</td>
                      <td className="longueur-required" style={{ textAlign: 'right', fontWeight: '500' }}>
                        {item.totalLongueurTotal.toFixed(2)}
                      </td>
                      <td className="difference" style={{
                        textAlign: 'right',
                        fontWeight: '600',
                        color: isPositive ? '#006400' : '#8B0000',
                        backgroundColor: isPositive ? 'rgba(144, 238, 144, 0.3)' : 'rgba(255, 182, 193, 0.3)',
                        padding: '8px'
                      }}>
                        {difference.toFixed(2)} {isPositive ? '(OK)' : '(Shortage)'}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>

        {
          stockData.length === 0 && (
            <div className="empty-state">
              <p>No stock data available</p>
            </div>
          )
        }

        {/* Demande Details Modal */}
        {this.renderDemandeModal()}
        
        {/* Import Excel Modal */}
        {this.renderImportModal()}
      </div >
    )
  }
}

RollQlaize.propTypes = {
  security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
  security: state.security
})

export default connect(mapStateToProps, {})(RollQlaize);
