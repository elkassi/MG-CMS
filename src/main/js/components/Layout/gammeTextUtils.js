export const DEFAULT_GAMME_TEXT_STYLE = {
	content: "TO VERIFY",
	labelX: 20,
	labelY: 30,
	labelSize: 28,
	fontFamily: "Arial",
	fontWeight: "900",
	fontStyle: "normal",
	fillColor: "#ff0000",
	rotation: 0,
	applyToPattern: false
}

const toComparable = value => (value == null ? "" : String(value).trim().toUpperCase())

export const sameTextValue = (first, second) => toComparable(first) === toComparable(second)

export const normalizeGammeTexts = (texts = []) => {
	return texts.map((text, index) => ({
		...DEFAULT_GAMME_TEXT_STYLE,
		...text,
		applyToPattern: text.applyToPattern === true,
		clientId: text.clientId || (text.id ? `db-${text.id}` : `local-${Date.now()}-${index}`)
	}))
}

export const getGammeTextsForPiece = (texts = [], piece) => {
	if (!piece || !piece.file) {
		return []
	}
	return texts.filter(text => {
		if (text.applyToPattern === true) {
			return text.pattern && piece.file.pattern && sameTextValue(text.pattern, piece.file.pattern)
		}
		return text.panelNumber && sameTextValue(text.panelNumber, piece.file.panelNumber)
	})
}

export const createGammeTextDraft = (piece, partNumber, applyToPattern, clientId) => {
	const pattern = piece && piece.file ? piece.file.pattern : null
	return {
		...DEFAULT_GAMME_TEXT_STYLE,
		clientId,
		partNumber,
		panelNumber: piece && piece.file ? piece.file.panelNumber : null,
		partNumberMaterial: piece && piece.file ? piece.file.partNumberMaterial : null,
		pattern,
		labelX: piece && piece.maxX ? Math.max(10, Math.round(piece.maxX / 2) - 80) : DEFAULT_GAMME_TEXT_STYLE.labelX,
		labelY: piece && piece.maxY ? Math.max(20, Math.round(piece.maxY / 4)) : DEFAULT_GAMME_TEXT_STYLE.labelY,
		applyToPattern: applyToPattern === true
	}
}

export const buildGammeTextResolveParams = (partNumber, pieces = []) => {
	const patterns = []
	pieces.forEach(piece => {
		if (piece && piece.file && piece.file.pattern && !patterns.some(pattern => sameTextValue(pattern, piece.file.pattern))) {
			patterns.push(piece.file.pattern)
		}
	})
	return {
		partNumber,
		patterns: patterns.join(",")
	}
}

export const buildGammeTextPayload = (texts = [], partNumber) => {
	return texts
		.filter(text => text && text.content && text.content.trim().length > 0)
		.map(text => ({
			id: text.id || null,
			partNumber: text.applyToPattern === true ? (text.partNumber || partNumber) : partNumber,
			panelNumber: text.panelNumber || null,
			partNumberMaterial: text.partNumberMaterial || null,
			pattern: text.pattern || null,
			content: text.content.trim(),
			labelX: text.labelX !== "" && text.labelX != null ? Number(text.labelX) : 0,
			labelY: text.labelY !== "" && text.labelY != null ? Number(text.labelY) : 0,
			labelSize: text.labelSize !== "" && text.labelSize != null ? Number(text.labelSize) : DEFAULT_GAMME_TEXT_STYLE.labelSize,
			fontFamily: text.fontFamily || DEFAULT_GAMME_TEXT_STYLE.fontFamily,
			fontWeight: text.fontWeight || DEFAULT_GAMME_TEXT_STYLE.fontWeight,
			fontStyle: text.fontStyle || DEFAULT_GAMME_TEXT_STYLE.fontStyle,
			fillColor: text.fillColor || DEFAULT_GAMME_TEXT_STYLE.fillColor,
			rotation: text.rotation !== "" && text.rotation != null ? Number(text.rotation) : 0,
			applyToPattern: text.applyToPattern === true
		}))
}
