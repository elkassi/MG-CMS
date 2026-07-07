

export const userFields = [
  {
    name: 'username',
    displayName: 'username',
    type: "text",
    required: true
  },
  {
    name: 'firstName',
    displayName: 'firstName',
    type: "text",
    required: true
  },
  {
    name: 'lastName',
    displayName: 'lastName',
    type: "text",
    required: true
  },
  {
    name: 'email',
    displayName: 'email',
    type: "text",
    required: true
  },
  {
    name: 'roles',
    displayName: 'role',
    type: "ROLE",
    required: true
  },
  {
    name: 'password',
    displayName: 'password',
    type: "password",
    required: true,
    notShowTable: true
  },
  {
    name: 'confirmPassword',
    displayName: 'Confirm Password',
    type: "password",
    required: true,
    notShowTable: true
  },
  {
    name: 'matricule',
    displayName: 'matricule',
    type: "text",
    required: true
  },
  {
    name: 'fonction',
    displayName: 'fonction',
    type: "text",
    required: false
  },
  {
    name: 'responsable',
    displayName: 'responsable',
    type: "object",
    required: false,
    formDisplayProperty: "nom"
  },

  {
    name: 'active',
    displayName: 'active',
    type: "boolean",
    required: true,
    defaultValue: true
  },
  {
    name: 'authPswChange',
    displayName: 'authPswChange',
    type: "boolean",
    required: true,
    defaultValue: false
  },
  {
    name: 'ipPrinter',
    displayName: 'ipPrinter',
    type: "text",
    required: false
  },
  {
    name: 'created_At',
    displayName: 'created at',
    required: false,
    type: "hidden"
  },
  {
    name: 'updated_At',
    displayName: 'updated at',
    required: false,
    type: "hidden"
  }
]

export const allColors = [
  "#82ca9d", // Light Green
  "#8884d8", // Light Purple
  "#ff7f50", // Coral
  "#87ceeb", // Sky Blue
  "#ffa07a", // Light Salmon
  "#6b8e23", // Olive Drab
  "#20b2aa", // Light Sea Green
  "#ff6347", // Tomato
  "#4682b4", // Steel Blue
  "#daa520", // Goldenrod
  "#ff4500", // Orange Red
  "#2e8b57", // Sea Green
  "#1e90ff", // Dodger Blue
  "#ff1493", // Deep Pink
  "#00ced1", // Dark Turquoise
  "#9400d3", // Dark Violet
  "#ff69b4", // Hot Pink
  "#8b0000", // Dark Red
  "#ffd700", // Gold
  "#7b68ee"  // Medium Slate Blue
];

export const jsonStyle = {
  propertyStyle: { color: 'red' },
  stringStyle: { color: 'green' },
  numberStyle: { color: 'darkorange' }
}

export const departementOption = [
  { label: "RH", value: "RH", num: "01" },
  { label: "Finance", value: "Finance", num: "02" },
  { label: "Logistique", value: "Logistique", num: "03" },
  { label: "Ingenierie", value: "Ingenierie", num: "04" }, { label: "CAD", value: "CAD", num: "04" }, { label: "Process coupe", value: "Process coupe", num: "04" },
  { label: "Production", value: "Production", num: "05" },
  { label: "Qualite coupe", value: "Qualite coupe", num: "06" },
  { label: "Qualite reception", value: "Qualite reception", num: "06" },
  { label: "Qualité couture", value: "Qualité couture", num: "06" },
  { label: "Maintenance", value: "Maintenance", num: "07" },
  { label: "IT", value: "IT", num: "09" },
  { label: "HSE", value: "HSE", num: "11" },
  { label: "CI", value: "CI", num: "12" },
]

export const optionsMatelassageEndroit = [
  { label: "--", value: "--" },
  { label: " -  / En Bas", value: " -  / En Bas" },
  { label: " -  / En Haut", value: " -  / En Haut" },
  { label: "Inverse", value: "Inverse" },
  { label: "Inverse / En Bas", value: "Inverse / En Bas" },
  { label: "Inverse / En Haut", value: "Inverse / En Haut" },
  { label: "Normal", value: "Normal" },
  { label: "Normal / En Bas", value: "Normal / En Bas" },
  { label: "Normal / En Haut", value: "Normal / En Haut" },
]

export const optionsReftissuProperty = [
  { label: "Biais", value: "Biais" },
  { label: "Sens", value: "Sens" },
  { label: "Autre", value: "Autre" },
]

export const optionsShift = [
  { label: "Shift 1", value: 1 },
  { label: "Shift 2", value: 2 },
  { label: "Shift 3", value: 3 },
]

export const optionZone = [
  { label: "Castilla", value: "Castilla" },
  { label: "Iberia", value: "Iberia" },
  { label: "TFZ", value: "TFZ" },
  { label: "Malabata", value: "Malabata" },
  { label: "Nejema", value: "Nejema" },
]

export const optionSiteQN = [
  { label: "--", value: "--" },
  { label: "Trim 1", value: "Trim 1" },
  { label: "Trim 2", value: "Trim 2" },
  { label: "FIP", value: "FIP" },
  { label: "Coupe", value: "Coupe" },
  { label: "Reception", value: "Reception" },
]

export const optionResultatQN = [
  { label: "--", value: "--" },
  { label: "Ok", value: "Ok" },
  { label: "Formation", value: "Formation" },
  { label: "Non ok", value: "Non ok" },
  { label: "Cloture", value: "Cloture" },
]

export const filterOptions = [
  { label: "startWith", value: "startWith" },
  { label: "endWith", value: "endWith" },
  { label: "equal", value: "equal" },
  { label: "notEqual", value: "notEqual" },
  { label: "contains", value: "contains" },
  { label: "greaterThan", value: "greaterThan" },
  { label: "lessThan", value: "lessThan" },
  { label: "isNull", value: "isNull" },
  { label: "isNotNull", value: "isNotNull" },

]

export const spliceTestMarker = ["-1M", "-3M", "-5M"]

export const lieuDetectionOptions = [
  { label: "All", value: "ALL" },
  { label: "1ere ligne", value: "1ere ligne" },
  { label: "milieu matelas", value: "milieu matelas" },
  { label: "dernier ligne", value: "dernier ligne" },
  { label: "dernier couche", value: "dernier couche" },
  { label: "Picking", value: "Picking" },
]

export const problemeResoluOption = [
  { label: "Oui", value: "Oui" },
  { label: "Non", value: "Non" },
  { label: "Provisoirement", value: "Provisoirement" },
]

export const optionStatus = [
  { label: "en attente", value: "en attente" },
  { label: "", value: "" },
]

export const optionCategory = [
  { label: "Matelassage", value: "Matelassage" },
  { label: "Coupe", value: "Coupe" },
  { label: "Gerber Matelassage", value: "Gerber Matelassage" },
  { label: "Gerber Coupe", value: "Gerber Coupe" },
  { label: "Picking", value: "Picking" },
  { label: "LSR", value: "LSR" },
  { label: "DXF", value: "DXF" },
  { label: "DIE", value: "DIE" },
  { label: "DIE Matelassage", value: "DIE Matelassage" },
]

export const optionTypes = [
  { value: "OK/NOK/NA", label: "OK/NOK/NA" },
  { value: "nombre", label: "nombre" },
  { value: "text", label: "text" },
]

export const optionSite = [
  { value: "TRIM1", label: "TRIM1" },
  { value: "TRIM2", label: "TRIM2" },
  { value: "TRIM4", label: "TRIM4" },
  { value: "FOAM", label: "FOAM" },
]

export const optionReponse = [
  { value: "En attente de la validation qualité coupe", label: "En attente de la validation qualité coupe" },
  { value: "En attente de la validation qualité réception", label: "En attente de la validation qualité réception" },
  { value: "En attente de la validation logistique", label: "En attente de la validation logistique" },
  { value: "Accepté", label: "Accepté" },
  { value: "Refusé", label: "Refusé" },
]

export const optionTypeDefaut = [
  { label: "Défaut coupe", value: "Défaut coupe" },
  { label: "Défaut fournisseur", value: "Défaut fournisseur" },
  { label: "Défaut logistique", value: "Défaut logistique" },
  { label: "Défaut CNC", value: "Défaut CNC" },

]

export const optionTypeDemandeChangementSerie = [
  { label: "Machine", value: "Machine", causes: ["Plan de charge", "Machine panne", "PM", "Test"] },
  { label: "Diviser Matelas", value: "Diviser Matelas", causes: ["Manque matière", "Nuance", "Shortage QLaize"] },
  // { label: "QLaize non dérogé", value: "QLaize non dérogé" },
  { label: "Overlaize", value: "Overlaize" },
  { label: "Changement de config", value: "Changement de config" },
  // longueur NOK saisie dans le champ laize (pas de nouvelle colonne)
  { label: "Erreur métrage", value: "Erreur métrage" }
]


export const floatRegex = /^[-+]?[0-9]*\.?[0-9]*$/;

export const metadata = {

  projet: {
    displayName: "Projet",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'nom', displayName: 'nom', type: "text", required: true, },
      { name: 'code', displayName: 'code', type: "text", required: true, },
      { name: 'zone', displayName: 'zone', type: "object", required: true, formDisplayProperty: "nom", formObject: "zone" },
      //site
      { name: 'site', displayName: 'site', type: "object", required: true, formDisplayProperty: "nom", formObject: "site" },
    ],
    fieldsFilter: [
      { name: 'nom', displayName: 'nom', type: "text", required: true, }
    ]
  },
  zone: {
    displayName: "Zone",
    operation: ["Add", "Delete"],
    fields: [
      { name: 'nom', displayName: 'nom', type: "text", required: true, },
      { name: 'description', displayName: 'description', type: "text", required: true, },
    ],
    fieldsFilter: [
      { name: 'nom', displayName: 'nom', type: "text", required: true, }
    ]
  },
  projetVersion: {
    displayName: "Codes des versions",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: 'projet', displayName: 'projet', type: "object", required: true, formDisplayProperty: "nom", formObject: "projet" },
      { name: 'version', displayName: 'version', type: "text", required: true },
      { name: 'code', displayName: 'code', type: "text", required: true },
    ],
    fieldsFilter: [
      { name: 'projet', displayName: 'projet', type: "text", required: true },
    ]
  },
  projetReftissu: {
    displayName: "Codes des reftissus",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: 'projet', displayName: 'projet', type: "object", required: true, formDisplayProperty: "nom", formObject: "projet" },
      { name: 'reftissu', displayName: 'reftissu', type: "text", required: true },
      { name: 'code', displayName: 'code', type: "text", required: true },
      { name: 'commentaire', displayName: 'commentaire', type: "text", required: true },
    ],
    fieldsFilter: [
      { name: 'projet', displayName: 'projet', type: "text", required: true },
    ]
  },
  markersOnlyCode: {
    displayName: "Marker Only Code",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'ID', type: "text", required: true, hideForm: true },
      { name: 'orderCode', displayName: 'Order Code', type: "text", required: true },
      { name: 'fabricType', displayName: 'Fabric Type', type: "text", required: true },
      { name: 'marker', displayName: 'Marker', type: "text", required: true },
      { name: 'layers', displayName: 'layers', type: "text", required: true },
      { name: 'multiply', displayName: 'multiply', type: "text", required: true },
      {
        name: 'status', displayName: 'Status', type: "option", optionsList: [
          { value: "waiting", label: "waiting" },
          { value: "on going", label: "on going" },
          { value: "finished", label: "finished" },
          { value: "startup test", label: "startup test" },
        ]
      }

    ]
  },
  intervention: {
    displayName: "Intervention",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: 'createdAt', displayName: 'date creation', type: "text", hideForm: true },
      { name: 'serie', displayName: 'Serie', type: "text", required: true },
      { name: 'sequence', displayName: 'Sequence', type: "text", required: true },
      { name: 'date', displayName: 'Date', type: "text", required: true },
      { name: 'shift', displayName: 'Shift', type: "text", required: true },
      { name: 'partNumberMaterial', displayName: 'Material', type: "text", required: true },
      { name: 'partNumberMaterialDescription', displayName: 'Material Description', type: "text", required: true },
      { name: 'debutArret', displayName: 'Debut Arret', type: "text", required: true },
      { name: 'debutIntervention', displayName: 'Debut Intervention', type: "text", required: true },
      { name: 'finIntervention', displayName: 'Fin Intervention', type: "text", required: true },
      { name: 'codeErreur', displayName: 'Code Erreur', type: "text", required: true },
      { name: 'codeArret', displayName: 'Code Arret', type: "object", required: true, formDisplayProperty: "code", formObject: "codeArret" },
      { name: 'codeArret', displayName: 'Motif Arret', type: "object", required: true, formDisplayProperty: "motifArret", formObject: "codeArret", hideForm: true },
      { name: 'codeCoupe', displayName: 'Code Coupe', type: "object", required: true, formDisplayProperty: "code", formObject: "codeDefaut" },
      { name: 'codeCoupe', displayName: 'Motif Coupe', type: "object", required: true, formDisplayProperty: "description", formObject: "codeDefaut", hideForm: true },
      { name: 'cause', displayName: 'Cause', type: "text", required: true },
      { name: 'action', displayName: 'Action', type: "text", required: true },
      { name: 'departement', displayName: 'Departement', type: "text", required: true },
      { name: 'problemeResolu', displayName: 'Probleme Resolu', type: "text", required: true },
      { name: 'matriculeEmetteur', displayName: 'Emetteur', type: "text", required: true },
      { name: 'matriculeResponsable', displayName: 'Responsable', type: "text", required: true },
      { name: 'solution', displayName: 'Solution', type: "object", required: true, formDisplayProperty: "code", formObject: "codeDefaut" },
      { name: 'solution', displayName: 'Description', type: "object", required: true, formDisplayProperty: "description", formObject: "codeDefaut", hideForm: true },
      { name: 'machine', displayName: 'machine', type: "text", required: true },
      { name: 'type', displayName: 'type', type: "text", required: true },
      { name: 'sousType', displayName: 'Sous Type', type: "text", required: true },
      // { name: 'numIntervention', displayName: 'numIntervention', type: "text", required: true },
      { name: 'descriptionDeffaillance', displayName: 'Deffaillance', type: "text", required: true },
      { name: 'descriptionArret', displayName: 'Arret', type: "text", required: true },
      { name: 'dateValidation', displayName: 'Date Validation', type: "text", required: true },
      { name: 'validerPar', displayName: 'Valider Par', type: "text", required: true },
    ]
  },

  machine: {
    displayName: "Machine",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'code', displayName: 'Code', type: "text", required: true, },
      { name: 'maxLaize', displayName: 'Max Laize', type: "text", required: true, reg: /^\d*\.?\d*$/ },
      { name: 'maxLength', displayName: 'Max Length', type: "text", required: true, reg: /^\d*\.?\d*$/ },
      { name: 'machineType', displayName: 'Type De Machine', type: "object", required: true, formDisplayProperty: "name", formObject: "machineType" }
    ],
    fieldsFilter: [
      { name: 'code', displayName: 'Code', type: "text" },
      { name: 'machineType', displayName: 'Type De Machine', type: "text" }
    ]
  },
  productionTable: {
    displayName: "Machine de Coupe",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'ID', type: "text", required: true, hideForm: true },
      { name: 'nom', displayName: 'Nom', type: "text", required: true },
      { name: 'zone', displayName: 'zone', type: "object", required: true, formDisplayProperty: "nom", formObject: "zone" },
      { name: 'pcMatelassage', displayName: 'PC Matelassage', type: "text", required: true },
      { name: 'pcCoupe', displayName: 'PC Coupe', type: "text", required: true },
      { name: 'machineType', displayName: 'Type De Machine', type: "object", required: true, formDisplayProperty: "name", formObject: "machineType" },
      { name: 'efficience', displayName: 'Efficience %', type: "text", required: true },
      // { name: 'ipImprimante', displayName: 'IP Imprimante', type: "text", required: true },
      { name: 'vibrationTime', displayName: 'Vibration Time', type: "text", required: true, hideForm: true },
      { name: 'vacuumTime', displayName: 'Vacuum Time', type: "text", required: true, hideForm: true },
      { name: 'bibliobus', displayName: 'Bibliobus', type: "text", required: true, hideForm: true },
      { name: 'serialNumber', displayName: 'Serial Number', type: "text", required: true },
      { name: 'type', displayName: 'Type', type: "text", required: true },
      { name: 'installationDate', displayName: 'Installation Date', type: "text", required: true },
      { name: 'autorisationAirbag', displayName: 'Autorisation Airbag', type: "boolean" },
      { name: 'forPls', displayName: 'PLS', type: "boolean" },

      { name: 'calibrageDrill1', displayName: 'Calibrage Drill 1', type: "text", required: true, hideForm: true },
      { name: 'calibrageDrill1Value', displayName: 'Calibrage Drill 1 Value', type: "text", required: true, hideForm: true },
      { name: 'calibrageDrill2', displayName: 'Calibrage Drill 2', type: "text", required: true, hideForm: true },
      { name: 'calibrageDrill2Value', displayName: 'Calibrage Drill 2 Value', type: "text", required: true, hideForm: true },
      { name: 'versionCMS', displayName: 'Version CMS', type: "text", required: true, hideForm: true },
      { name: 'versionCMSCoupe', displayName: 'Version CMS Coupe', type: "text", required: true, hideForm: true },
    ],
  },
  codeErreur: {
    displayName: "Code Erreur",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'ID', type: "text", required: true, hideForm: true },
      { name: 'code', displayName: 'code', type: "text", required: true },
      { name: 'designation', displayName: 'designation', type: "text", required: true },
      { name: 'rootCause', displayName: 'rootCause', type: "text", required: true },
      { name: 'actionPossible', displayName: 'actionPossible', type: "text", required: true },
      { name: 'internevantOperateur', displayName: 'internevantOperateur', type: "boolean", required: true },
      { name: 'internevantTechLear', displayName: 'internevantTechLear', type: "boolean", required: true },
      { name: 'internevantTechLectra', displayName: 'internevantTechLectra', type: "boolean", required: true },
    ],
    fieldsFilter: [
      { name: 'code', displayName: 'code', type: "text" },
    ]
  },
  codeArret: {
    displayName: "Code Arret",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'code', displayName: 'Code', type: "text", required: true },
      { name: 'departement', displayName: 'Departement', type: "text", required: true },
      { name: 'typeArret', displayName: 'Type Arret', type: "text", required: true },
      { name: 'motifArret', displayName: 'Motif Arret', type: "text", required: true },
      { name: 'email', displayName: 'Email', type: "text", required: true },
    ],
    fieldsFilter: [
      { name: 'code', displayName: 'code', type: "text" },
    ]
  },
  codeDefaut: {
    displayName: "Code Defaut",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'code', displayName: 'Code', type: "text", required: true },
      { name: 'description', displayName: 'Description', type: "text", required: true },
      { name: 'departement', displayName: 'Departement', type: "text", required: true },
      { name: 'type', displayName: 'Type', type: "text", required: true },
      {
        name: 'origin', displayName: 'Origin', type: "option", optionsList: [
          { value: "Coupe", label: "Coupe" },
          { value: "Fournisseur", label: "Fournisseur" },
        ]
      }
    ],
    fieldsFilter: [
      { name: 'code', displayName: 'code', type: "text" },
    ]
  },
  codeScrap: {
    displayName: "Code Scrap",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'code', displayName: 'Code', type: "text", required: true },
      { name: 'description', displayName: 'Description', type: "text", required: true },
      { name: 'departement', displayName: 'Departement', type: "text", required: true },
      { name: 'type', displayName: 'Type', type: "text", required: true },
    ],
    fieldsFilter: [
      { name: 'code', displayName: 'code', type: "text" },
    ]
  },
  machineType: {
    displayName: "Machine Type",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'name', displayName: 'name', type: "text", required: true, },
      { name: 'description', displayName: 'description', type: "text", required: true },
    ]
  },
  partNumberBoom: {
    displayName: "Part Number",
    operation: [],
    fields: [
      { name: 'partNumber', displayName: 'Part Number', type: "text", required: true },
      { name: 'description', displayName: 'description', type: "text" },
      { name: 'partNumberMaterial', displayName: 'Part Number Material', type: "text", required: true },
      { name: 'partNumberMaterialDescription', displayName: 'description', type: "text" },
      { name: 'project', displayName: 'Project', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'item', displayName: 'Item', type: "text" },
      { name: 'quantityPer', displayName: 'Quantity Per', type: "text" },
    ],
    fieldsFilter: [
      { name: 'partNumber', displayName: 'Part Number', type: "text" },
      { name: 'partNumberMaterial', displayName: 'Part Number Material', type: "text" },
      { name: 'project', displayName: 'Project', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'item', displayName: 'Item', type: "text" },
    ]
  },
  partNumberMaterialConfig: {
    displayName: "Part Number Material",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'partNumberMaterial', displayName: 'Part Number Material', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/ },
      { name: 'description', displayName: 'description', type: "text" },
      { name: 'vitesse', displayName: 'vitesse', type: "text" },
      { name: 'rotation', displayName: 'rotation', type: "text" },
      { name: 'plaque', displayName: 'plaque', type: "text" },
      { name: 'tauxScrap', displayName: 'tauxScrap', type: "text" },
      { name: 'matelassageEndroit', displayName: 'Matelassage Endroit', type: "option", optionsList: [...optionsMatelassageEndroit] },
      { name: 'commentaire', displayName: 'commentaire', type: "text" },
      { name: 'margeLaizeMin', displayName: 'Marge Laize Min (cm)', type: "text" },
      { name: 'margeLaizeMax', displayName: 'Marge Laize Max (cm)', type: "text" },

      { name: 'validated0BF', displayName: 'Validated 0BF', type: "boolean" },
      { name: 'validatedIP6', displayName: 'Validated IP6', type: "boolean" },
      // buffer1IP6 buffer2IP6
      { name: 'buffer1IP6', displayName: 'Buffer 1 IP6', type: "text" },
      { name: 'buffer2IP6', displayName: 'Buffer 2 IP6', type: "text" },

      { name: 'fipDev', displayName: 'FIP dev', type: "boolean" },

      { name: 'weightUnit', displayName: 'Poids/m² (kg)', type: "text" },


      { name: 'createdBy', displayName: 'createdBy', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'createdAt', displayName: 'createdAt', type: "text", hideForm: true },
      { name: 'lastUsedDate', displayName: 'Last Used Date', type: "text", hideForm: true },
    ],
    fieldsFilter: [
      { name: 'partNumberMaterial', displayName: 'Part Number Material', type: "text" },
    ]
  },

  partNumberMaterialConfigData: {
    displayName: "Part Number Material",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'partNumberMaterial', displayName: 'Part Number Material', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/ },
      { name: 'description', displayName: 'description', type: "text" },
      { name: 'vitesse', displayName: 'vitesse', type: "text" },
      { name: 'rotation', displayName: 'rotation', type: "text" },
      { name: 'plaque', displayName: 'plaque', type: "text" },
      { name: 'tauxScrap', displayName: 'tauxScrap', type: "text" },
      { name: 'matelassageEndroit', displayName: 'Matelassage Endroit', type: "option", optionsList: [...optionsMatelassageEndroit] },
      { name: 'commentaire', displayName: 'commentaire', type: "text" },
      { name: 'margeLaizeMin', displayName: 'Marge Laize Min (cm)', type: "text" },
      { name: 'margeLaizeMax', displayName: 'Marge Laize Max (cm)', type: "text" },
      { name: 'validated0BF', displayName: 'Validated 0BF', type: "boolean" },
      { name: 'validatedIP6', displayName: 'Validated IP6', type: "boolean" },
      // buffer1IP6 buffer2IP6
      { name: 'buffer1IP6', displayName: 'Buffer 1 IP6', type: "text" },
      { name: 'buffer2IP6', displayName: 'Buffer 2 IP6', type: "text" },

      { name: 'fipDev', displayName: 'FIP dev', type: "boolean" },

      { name: 'weightUnit', displayName: 'Poids/m² (kg)', type: "text" },

    ],
  },

  pointage: {
    displayName: "Pointage des interventions",
    operation: ["Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: 'date', displayName: 'date', type: "text" },
      { name: 'dateFin', displayName: 'dateFin', type: "text" },
      { name: 'matricule', displayName: 'matricule', type: "text" },
      { name: 'fullName', displayName: 'fullName', type: "text" },
      { name: 'poste', displayName: 'poste', type: "text" },
      { name: 'type', displayName: 'type', type: "text" },
      { name: 'departement', displayName: 'departement', type: "text" },
      { name: 'intervention', displayName: 'intervention', type: "text" },
    ],
  },

  stockStatusReport: {
    displayName: "Stock Status Report - QAD",
    tableSize: 100,
    operation: [],
    fields: [
      { name: 'itemNumber', displayName: 'itemNumber', type: "text" },
      { name: 'um', displayName: 'um', type: "text" },
      { name: 'abc', displayName: 'abc', type: "text" },
      { name: 'site', displayName: 'site', type: "text" },
      { name: 'lastCnt', displayName: 'lastCnt', type: "text" },
      { name: 'location', displayName: 'location', type: "text" },
      { name: 'ref', displayName: 'ref', type: "text" },
      { name: 'qtyOnHand', displayName: 'qtyOnHand', type: "text" },
      { name: 'status', displayName: 'status', type: "text" },
      { name: 'lastUpdated', displayName: 'lastUpdated', type: "text" },
      { name: 'isDeleted', displayName: 'Deleted', type: "boolean" },
    ],
  },

  rapportUsageReport: {
    displayName: "Rapport Usage / BOM (historique)",
    tableSize: 100,
    firstOrderProperty: "cuttingRequest_sequence",
    operation: [],
    fields: [
      { name: 'cuttingPlanId', displayName: 'Cutting Plan Id', type: "number" },
      { name: 'cuttingRequest_sequence', displayName: 'Sequence', type: "text" },
      { name: 'dateDebutMatelassage', displayName: 'Debut Matelassage', type: "text" },
      { name: 'dateFinMatelassage', displayName: 'Fin Matelassage', type: "text" },
      { name: 'dateDebutCoupe', displayName: 'Debut Coupe', type: "text" },
      { name: 'dateFinCoupe', displayName: 'Fin Coupe', type: "text" },
      { name: 'confirmReftissu', displayName: 'Reftissu', type: "text" },
      { name: 'description', displayName: 'Description', type: "text" },
      { name: 'totalConsommationPlan', displayName: 'Consommation Plan', type: "number" },
      { name: 'overlap', displayName: 'Overlap', type: "number" },
      { name: 'nonUtitlse', displayName: 'Non Utitlse', type: "number" },
      { name: 'defaut', displayName: 'Defaut', type: "number" },
      { name: 'totalUsage', displayName: 'Total Usage', type: "number" },
      { name: 'excess', displayName: 'Excess', type: "number" },
      { name: 'finalUsage', displayName: 'Final Usage', type: "number" },
      { name: 'qadUsage', displayName: 'Qad Usage', type: "number" },
      { name: 'variance', displayName: 'Variance', type: "number" },
      { name: 'statusMatelassage', displayName: 'Statut Matelassage', type: "text" },
      { name: 'lastUpdated', displayName: 'Derniere maj', type: "text" },
    ],
  },

  cuttingPlan: {
    displayName: "Plan de coupe",
    operation: ["Add", "Edit", "Delete", "Export"],
    fields: [
      { name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: 'cmsId', displayName: 'CMS Id', type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: 'projet', displayName: 'Projet', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'description', displayName: 'Description', type: "text" },
      { name: 'version2', displayName: 'Version2', type: "text", hideTable: true },
      { name: 'definition', displayName: 'Definition', type: "text" },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
      { name: 'createdBy', displayName: 'créer par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'updatedAt', displayName: 'Date de modification', type: "text", hideForm: true },
      { name: 'updatedBy', displayName: 'Modifier par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'enabled', displayName: 'enabled', type: "boolean" },
      { name: 'enabledAt', displayName: 'enabledAt', type: "text", hideForm: true },
      { name: 'enabledBy', displayName: 'enabledBy', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'type', displayName: 'type', type: "text" },
      { name: 'copyId', displayName: 'Copie Id', type: "text" },
      { name: 'startDate', displayName: 'Date de début', type: "text" },
      { name: 'endDate', displayName: 'Date de fin', type: "text" },
      { name: 'commentaire', displayName: 'Commentaire', type: "text" },
      { name: 'consommation', displayName: 'Consommation', type: "boolean" },
      //alertMessages
      { name: 'alertMessages', displayName: 'Alert Messages', type: "text" },
      { name: 'foam', displayName: 'Foam', type: "boolean" },
    ],
    fieldsFilter: [
      { name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'cmsId', displayName: 'CMS Id', type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: 'projet', displayName: 'Projet', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'description', displayName: 'Description', type: "text" },
      { name: 'version2', displayName: 'Version2', type: "text", hideTable: true },
      { name: 'definition', displayName: 'Definition', type: "text" },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
      { name: 'createdBy', displayName: 'créer par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'updatedAt', displayName: 'Date de modification', type: "text", hideForm: true },
      { name: 'updatedBy', displayName: 'Modifier par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'enabled', displayName: 'enabled', type: "boolean" },
      { name: 'enabledAt', displayName: 'enabledAt', type: "text", hideForm: true },
      { name: 'enabledBy.matricule', displayName: 'enabledBy', type: "text" },
      { name: 'type', displayName: 'type', type: "text" },
      { name: 'copyId', displayName: 'Copie Id', type: "text" },
      { name: 'startDate', displayName: 'Date de début', type: "text" },
      { name: 'endDate', displayName: 'Date de fin', type: "text" },
      { name: 'commentaire', displayName: 'Commentaire', type: "text" },
      { name: 'consommation', displayName: 'Consommation', type: "boolean" },
      { name: 'foam', displayName: 'Foam', type: "boolean" },
    ]
  },
  cuttingPlanData: {
    displayName: "Données plan de coupe",
    operation: [],
    fields: [
      { name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'cmsId', displayName: 'CMS Id', type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: 'projet', displayName: 'Projet', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'description', displayName: 'Description', type: "text" },
      { name: 'version2', displayName: 'Version2', type: "text", hideTable: true },
      { name: 'definition', displayName: 'Definition', type: "text" },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
      { name: 'createdBy', displayName: 'créer par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'updatedAt', displayName: 'Date de modification', type: "text", hideForm: true },
      { name: 'updatedBy', displayName: 'Modifier par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'enabled', displayName: 'enabled', type: "boolean" },
      { name: 'enabledAt', displayName: 'enabledAt', type: "text", hideForm: true },
      { name: 'enabledBy.matricule', displayName: 'enabledBy', type: "text" },
      { name: 'type', displayName: 'type', type: "text" },
      { name: 'copyId', displayName: 'Copie Id', type: "text" },
      { name: 'startDate', displayName: 'Date de début', type: "text" },
      { name: 'endDate', displayName: 'Date de fin', type: "text" },
      { name: 'commentaire', displayName: 'Commentaire', type: "text" },
      { name: 'consommation', displayName: 'Consommation', type: "boolean" },
      { name: 'verification1', displayName: 'Verification 1', type: "text" },
      { name: 'verification2', displayName: 'Verification 2', type: "text" },
      { name: 'verification3', displayName: 'Verification 3', type: "text" },
      { name: 'verification4', displayName: 'Verification 4', type: "text" },
      { name: 'verification5', displayName: 'Verification 5', type: "text" },
      { name: 'verification6', displayName: 'Verification 6', type: "text" },
      { name: 'alertMessages', displayName: 'Alert Messages', type: "text" },

    ]
  },

  cuttingPlanPartNumberData: {
    displayName: "Données plan de coupe - Part Number",
    operation: [],
    fields: [
      { name: 'partNumber', displayName: 'Part Number', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'cuttingPlan', displayName: 'Plan de coupe', type: "number" },
      { name: 'description', displayName: 'Description', type: "text" },
      { name: 'item', displayName: 'Item', type: "text" },
      { name: 'quantityPer', displayName: 'Quantité par', type: "number" },
      { name: 'quantity', displayName: 'Quantité', type: "number" },
    ]
  },

  cuttingPlanMaterialData: {
    displayName: "Données plan de coupe - Matériel",
    operation: [],
    fields: [
      { name: 'cuttingPlan', displayName: 'Plan de coupe', type: "number" },
      { name: 'partNumberMaterial', displayName: 'N° de pièce matériel', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'description', displayName: 'Description', type: "text" },
      { name: 'vitesse', displayName: 'Vitesse', type: "number" },
      { name: 'rotation', displayName: 'Rotation', type: "text" },
      { name: 'plaque', displayName: 'Plaque', type: "number" },
      { name: 'tauxScrap', displayName: 'Taux scrap', type: "text" },
      { name: 'matelassageEndroit', displayName: 'Matelassage endroit', type: "text" },
    ]
  },

  cuttingPlanMaterialPlacementData: {
    displayName: "Données plan de coupe - Placement",
    operation: [],
    fields: [
      { name: 'placement', displayName: 'Placement', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'cuttingPlan', displayName: 'Plan de coupe', type: "number" },
      { name: 'partNumberMaterial', displayName: 'N° de pièce matériel', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'partNumbers', displayName: 'N° de pièce', type: "text" },
      { name: 'groupPlacement', displayName: 'Groupe placement', type: "number" },
      { name: 'activated', displayName: 'Activé', type: "boolean" },
      { name: 'machine', displayName: 'Machine', type: "text" },
      { name: 'maxPlie', displayName: 'Max plie', type: "number" },
      { name: 'maxPlieDrill', displayName: 'Max plie drill', type: "number" },
      { name: 'maxDrill', displayName: 'Max drill', type: "number" },
      { name: 'nbrCouche', displayName: 'Nbr couche', type: "number" },
      { name: 'config', displayName: 'Config', type: "text" },
      { name: 'drill', displayName: 'Drill', type: "text" },
      { name: 'category', displayName: 'Catégorie', type: "text" },
      { name: 'laize', displayName: 'Laize', type: "number" },
      { name: 'longueur', displayName: 'Longueur', type: "number" },
      { name: 'longueurMatelas', displayName: 'Longueur matelas', type: "number" },
      { name: 'perimetre', displayName: 'Périmètre', type: "number" },
      { name: 'tempsDeCoupe', displayName: 'Temps de coupe', type: "number" },
      { name: 'pliesConfig', displayName: 'Plies config', type: "text" },
      { name: 'pliesConfigMarge', displayName: 'Plies config marge', type: "text" },
      { name: 'pliesConfigMarge', displayName: 'Plies config marge', type: "text" },
      { name: "espaceRelarge", displayName: "Espace relarge", type: "text" },
      { name: "rotation", displayName: "Rotation", type: "text" },
      { name: "verifEndroit", displayName: "Verif Endroit", type: "text" },
    ]
  },
  cuttingPlanCombination: {
    displayName: "Combinaison plan de coupe",
    operation: ["Add", "Edit", "Delete", "Export"],
    fields: [
      { name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'projet', displayName: 'Projet', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'description', displayName: 'Description', type: "text" },
      { name: 'version2', displayName: 'Version2', type: "text", hideTable: true },
      { name: 'definition', displayName: 'Definition', type: "text" },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
      { name: 'createdBy', displayName: 'créer par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'updatedAt', displayName: 'Date de modification', type: "text", hideForm: true },
      { name: 'updatedBy', displayName: 'Modifier par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'enabled', displayName: 'enabled', type: "boolean" },
      { name: 'enabledAt', displayName: 'enabledAt', type: "text", hideForm: true },
      { name: 'enabledBy', displayName: 'enabledBy', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'type', displayName: 'type', type: "text" },
      { name: 'copyId', displayName: 'Copie Id', type: "text" },
      { name: 'startDate', displayName: 'Date de début', type: "text" },
      { name: 'endDate', displayName: 'Date de fin', type: "text" },
      { name: 'commentaire', displayName: 'Commentaire', type: "text" },
    ],
    fieldsFilter: [
      { name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'projet', displayName: 'Projet', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'description', displayName: 'Description', type: "text" },
      { name: 'version2', displayName: 'Version2', type: "text", hideTable: true },
      { name: 'definition', displayName: 'Definition', type: "text" },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
      { name: 'createdBy', displayName: 'créer par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'updatedAt', displayName: 'Date de modification', type: "text", hideForm: true },
      { name: 'updatedBy', displayName: 'Modifier par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'enabled', displayName: 'enabled', type: "boolean" },
      { name: 'enabledAt', displayName: 'enabledAt', type: "text", hideForm: true },
      { name: 'enabledBy.matricule', displayName: 'enabledBy', type: "text" },
      { name: 'type', displayName: 'type', type: "text" },
      { name: 'copyId', displayName: 'Copie Id', type: "text" },
      { name: 'startDate', displayName: 'Date de début', type: "text" },
      { name: 'endDate', displayName: 'Date de fin', type: "text" },
      { name: 'commentaire', displayName: 'Commentaire', type: "text" },
    ]
  },
  ctcFiles: {
    displayName: "CTC Files",
    operation: ["Add", "Edit", "Delete", "Import", "Export", "Supprimer"], // "Export",
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'partNumberCover', displayName: 'Part Number Cover', type: "text" },
      { name: 'partNumberCoverDesciption', displayName: 'PN Cover Description', type: "text" },
      { name: 'panelNumber', displayName: 'Panel Number', type: "text" },
      { name: 'semiFinishedGoodPartNumber', displayName: "Semi-Finished Goods' PN", type: "text" },
      { name: 'pattern', displayName: 'Pattern', type: "text" },
      { name: 'partNumberMaterial', displayName: 'Part Number Material', type: "text" },
      { name: 'partNumberMaterialDescription', displayName: 'PN Material Description', type: "text" },
      { name: 'type', displayName: 'type', type: "text" },
      { name: 'ecnNumber', displayName: 'ECN Number', type: "text" },
      { name: 'quantity', displayName: 'Quantité', type: "text" },
      { name: 'projet', displayName: 'Projet', type: "text" },
      { name: 'toleranceDrill', displayName: 'Tolerance Drill', type: "text" },

      { name: 'min1', displayName: 'Min 1', type: "text" },
      { name: 'max1', displayName: 'Max 1', type: "text" },
      { name: 'min2', displayName: 'Min 2', type: "text" },
      { name: 'max2', displayName: 'Max 2', type: "text" },
      //toleranceDrill
      { name: 'addedBy', displayName: 'addedBy', type: "text", hideForm: true },
      { name: 'updatedBy', displayName: 'updatedBy', type: "text", hideForm: true },
      { name: 'createdAt', displayName: 'createdAt', type: "text", hideForm: true },
      { name: 'updatedAt', displayName: 'updatedAt', type: "text", hideForm: true },
      { name: 'pltFound', displayName: 'PLT Found', type: "boolean", hideForm: true },
    ],
  },
  filesHistory: {
    displayName: "CTC Files History",
    operation: [], // "Export",
    firstOrderProperty: "updatedAt",
    fields: [
      { name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
      { name: 'typeOperation', displayName: 'typeOperation', type: "text" },
      { name: 'changement', displayName: 'changement', type: "text" },
      { name: 'updatedBy', displayName: 'updatedBy', type: "text", hideForm: true },
      { name: 'updatedAt', displayName: 'updatedAt', type: "text", hideForm: true },
    ],
    fieldsFilter: [
      { name: 'typeOperation', displayName: 'Type Operation', type: "text" },
      { name: 'changement', displayName: 'Changement', type: "text" },
      { name: 'updatedBy', displayName: 'Updated By', type: "text", hideForm: true },
      { name: 'updatedAt', displayName: 'Updated At', type: "text", hideForm: true },
    ]
  },
  qn: {
    displayName: "Flash qualité",
    operation: ["Add", "Edit", "Delete"], // "Export",
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'numeroQn', displayName: 'N° QN', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/ },
      { name: 'site', displayName: 'Site', type: "text", required: true, type: "option", optionsList: [...optionSiteQN] },
      { name: 'projet', displayName: 'Projet', type: "object", required: true, formDisplayProperty: "nom", formObject: "projet", },
      { name: 'reftissu', displayName: 'Reftissu', type: "text" },
      { name: 'description', displayName: "Description", type: "text" },
      {
        name: 'typeDefaut', displayName: 'Type Défaut', type: "option", optionsList: [
          { value: 'Défaut Coupe', label: 'Défaut Coupe' },
          { value: 'Défaut Fournisseur', label: 'Défaut Fournisseur' },
          { label: "Défaut logistique", value: "Défaut logistique" },
          { label: "Défaut CNC", value: "Défaut CNC" },
        ]
      },
      {
        name: 'appliquerSur', displayName: 'Appliquer sur', type: "option", optionsList: [
          { value: 'Matelassage', label: 'Matelassage' },
          { value: 'Coupe', label: 'Coupe' },
          { value: 'Les deux', label: 'Les deux (Matelassage + Coupe)' },
        ]
      },
      { name: 'descriptionDefaut', displayName: 'Description Défaut', type: "text" },
      { name: 'placement', displayName: 'Placement', type: "text" },
      { name: 'digit', displayName: 'Digit', type: "text" },
      { name: 'resultat', displayName: "Resultat", type: "option", optionsList: [...optionResultatQN] },
      { name: 'image', displayName: 'Image', type: "image" },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
    ],
    fieldsFilter: [
      { name: 'numeroQn', displayName: 'numeroQn', type: "text" },
    ]
  },
  cuttingRequestData: {
    displayName: "Demande de coupe",
    operation: ["Add", "Edit"], // "Export",
    firstOrderProperty: "sequence",
    fields: [
      { name: 'sequence', displayName: 'Sequence', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/ },
      { name: "cuttingPlanId", displayName: "Cutting Plan", type: "text", reg: /^[0-9]*$/ },
      { name: 'projet', displayName: 'Projet', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'modele', displayName: 'Modèle', type: "text" },
      { name: "definition", displayName: "Définition", type: "text" },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
      { name: 'createdBy', displayName: 'créer par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'planningDate', displayName: 'Date de planification', type: "text", required: true },
      { name: 'shift', displayName: 'shift', type: "text", required: true, reg: /^[0-9]*$/ },
      { name: 'zone', displayName: 'zone', type: "object", formDisplayProperty: "nom", formObject: "zone", required: true },
    ],
    fieldsFilter: [
      { name: 'sequence', displayName: 'Sequence', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/ },
      { name: "cuttingPlanId", displayName: "Cutting Plan", type: "text", reg: /^[0-9]*$/ },
      { name: 'projet', displayName: 'Projet', type: "text" },
      { name: 'version', displayName: 'Version', type: "text" },
      { name: 'modele', displayName: 'Modèle', type: "text" },
      { name: "definition", displayName: "Définition", type: "text" },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
      { name: 'createdBy', displayName: 'créer par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'planningDate', displayName: 'Date de planification', type: "date", required: true },
      { name: 'shift', displayName: 'shift', type: "text", required: true, reg: /^[0-9]*$/ },
      { name: 'zone', displayName: 'zone', type: "object", formDisplayProperty: "nom", formObject: "zone", required: true },
    ]
  },
  // private String partNumber;
  // private String description;
  // private String item;
  // private String itemcode5;
  // private String leatherKit;
  // private String supplierKit;
  // private Double heightRow;

  // private String image;
  // private Integer imageHeight;

  // @ManyToOne
  // private User createdBy;
  // private LocalDateTime createdAt;

  // @ManyToOne
  // private User updatedBy;
  // private LocalDateTime updatedAt;
  gammeTechnique: {
    displayName: "Gamme Technique",
    operation: [], // "Export",
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'partNumber', displayName: 'Part Number', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/ },
      { name: 'createdAt', displayName: 'Date de création', type: "text", hideForm: true },
      { name: 'createdBy', displayName: 'Créer par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'updatedAt', displayName: 'Date de modification', type: "text", hideForm: true },
      { name: 'updatedBy', displayName: 'Modifier par', type: "object", formDisplayProperty: "matricule", formObject: "user", hideForm: true },
      { name: 'description', displayName: 'Description', type: "text", required: true },
      { name: 'item', displayName: 'Item', type: "text", required: true },
      { name: 'itemcode5', displayName: 'Itemcode5', type: "text", required: true },
      { name: 'leatherKit', displayName: 'Leather Kit', type: "text", required: true },
      { name: 'supplierKit', displayName: 'Supplier Kit', type: "text", required: true },
      { name: 'heightRow', displayName: 'Height Row', type: "text", required: true },
      { name: 'image', displayName: 'Image', type: "image" },
      { name: 'imageHeight', displayName: 'Image Height', type: "text", required: true },
    ],
  },

  cuttingRequestSerieData: {
    displayName: "Demande de coupe série",
    operation: ["Add", "Edit", "Delete"], // "Export",
    firstOrderProperty: "serie",
    fields: [
      { name: 'serie', displayName: 'Série', type: "text", required: true, reg: /^[0-9]*$/ },
      { name: "sequence", displayName: "Sequence", type: "text", reg: /^[0-9]*$/ },
      { name: "partNumberMaterial", displayName: "Part Number Material", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "matelassageEndroit", displayName: "Matelassage Endroit", type: "text" },
      { name: "longueur", displayName: "Longueur", type: "text" },
      { name: "partNumbers", displayName: "Part Numbers", type: "text" },
      { name: "groupPlacement", displayName: "Group Placement", type: "text" },
      { name: "activated", displayName: "Activated", type: "boolean" },
      { name: "machine", displayName: "Machine", type: "text" },
      { name: "maxPlie", displayName: "Max Pli", type: "text" },
      { name: "maxPlieDrill", displayName: "Max Pli Drill", type: "text" },
      { name: "maxDrill", displayName: "Max Drill", type: "text" },
      { name: "nbrCouche", displayName: "Nbr Couche", type: "text" },
      { name: "placement", displayName: "Placement", type: "text" },
      { name: "laize", displayName: "Laize", type: "text" },
      { name: "config", displayName: "Config", type: "text" },
      { name: "drill", displayName: "Drill", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text" },
      { name: "planningDate", displayName: "Planning Date", type: "text" },
      { name: "shift", displayName: "Shift", type: "text" },
      { name: "ind", displayName: "Ind", type: "text" },
      { name: "zoneMatelassage", displayName: "Zone Matelassage", type: "text" },
      { name: "tableMatelassage", displayName: "Table Matelassage", type: "text" },
      { name: "matelasseur1", displayName: "Matelasseur 1", type: "text" },
      { name: "matelasseur2", displayName: "Matelasseur 2", type: "text" },
      { name: "dateDebutMatelassage", displayName: "Date Debut Matelassage", type: "text" },
      { name: "dateFinMatelassage", displayName: "Date Fin Matelassage", type: "text" },
      { name: "statusMatelassage", displayName: "Status Matelassage", type: "text" },
      { name: "zoneCoupe", displayName: "Zone Coupe", type: "text" },
      { name: "tableCoupe", displayName: "Table Coupe", type: "text" },
      { name: "coupeur1", displayName: "Coupeur 1", type: "text" },
      { name: "coupeur2", displayName: "Coupeur 2", type: "text" },
      { name: "statusCoupe", displayName: "Status Coupe", type: "text" },
      { name: "dateDebutCoupe", displayName: "Date Debut Coupe", type: "text" },
      { name: "dateFinCoupe", displayName: "Date Fin Coupe", type: "text" },
      { name: "autoCoupe", displayName: "Auto Coupe", type: "text" },
      { name: "nbrPiece", displayName: "Nbr Piece", type: "text" },
      { name: "tableQualite", displayName: "Table Qualite", type: "text" },
      { name: "controlleur", displayName: "Controlleur", type: "text" },
      { name: "matriculePicking", displayName: "Matricule Picking", type: "text" },
      { name: "qteNonConforme", displayName: "Qte Non Conforme", type: "text" },
      { name: "codeDefaut", displayName: "Code Defaut", type: "object", formDisplayProperty: "code", formObject: "codeDefaut", required: true },
      { name: "qteScrap", displayName: "Qte Scrap", type: "text" },
      { name: "codeScrap", displayName: "Code Scrap", type: "object", formDisplayProperty: "code", formObject: "codeScrap", required: true },
      { name: "nbrPieceTotal", displayName: "Nbr Piece Total", type: "text" },
      { name: "lieuDetection", displayName: "Lieu Detection", type: "text" },
      { name: "codeDefautAdditionnel", displayName: "Code Defaut Additionnel", type: "object", formDisplayProperty: "code", formObject: "codeDefaut", required: true },
      { name: "premierPaquet", displayName: "Premier Paquet", type: "text" },
      //milieuPaquet
      { name: "milieuPaquet", displayName: "Milieu Paquet", type: "text" },
      { name: "dernierPaquet", displayName: "Dernier Paquet", type: "text" },
      { name: "verificationDrill", displayName: "Verification Drill", type: "text" },
      { name: "verificationDrill2", displayName: "Verification Drill 2", type: "text" },
      { name: "retourMagasin", displayName: "Retour Magasin", type: "text" },
    ]
  },

  cuttingRequestSerieRouleauData: {
    displayName: "Cutting Request Serie Rouleau",
    operation: ["Add", "Edit", "Delete"], // "Export",
    firstOrderProperty: "createdAt",
    fields: [
      { name: "id", displayName: "Id", type: "number", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "serie", displayName: "Serie", type: "text" },
      { name: "confirmReftissu", displayName: "Confirm Reftissu", type: "text" },
      { name: "machine", displayName: "machine", type: "text" },
      { name: "location", displayName: "location", type: "text" },
      { name: "sequence", displayName: "sequence", type: "text" },
      { name: "idRouleau", displayName: "Id Rouleau", type: "text" },
      { name: "lotFrs", displayName: "Lot Frs", type: "text" },
      { name: "metrage", displayName: "Metrage", type: "text", reg: floatRegex },
      { name: "laize", displayName: "Laize", type: "text", reg: floatRegex },
      { name: "nbrCouche", displayName: "Nbr Couche", type: "text", reg: floatRegex },
      { name: "longueurPremierCouche", displayName: "Longueur Premier Couche", type: "text", reg: floatRegex },
      { name: "longueurCoucheOverlap", displayName: "Longueur Couche Overlap", type: "text", reg: floatRegex },
      { name: "defaut", displayName: "Defaut", type: "text", reg: floatRegex },
      { name: "nonUtitlse", displayName: "Non Utitlse", type: "text", reg: floatRegex },
      //private String nuance;
      { name: "nuance", displayName: "Nuance", type: "text" },
      { name: "retour", displayName: "Retour", type: "text", reg: floatRegex },
      { name: "excess", displayName: "Excess", type: "text", reg: floatRegex },
      { name: "totalUsage", displayName: "Total Usage", type: "text", reg: floatRegex },
      { name: "confirmRetour", displayName: "Confirm Retour", type: "boolean" },
      { name: "defautCode", displayName: "Defaut Code", type: "text" },
      { name: "deblockedDate", displayName: "Deblocked Date", type: "text" },
      { name: "deblockedBy", displayName: "Deblocked By", type: "text" },
      { name: "deblockedMetrage", displayName: "Deblocked Metrage", type: "text", reg: floatRegex },
      { name: "createdAt", displayName: "Created At", type: "date", hideForm: true },
      { name: "updatedAt", displayName: "Updated At", type: "date", hideForm: true },
      { name: "createdBy", displayName: "createdBy", type: "text" },
      { name: "updatedBy", displayName: "updatedBy", type: "text" },
      { name: "overlap1", displayName: "Overlap 1", type: "text", reg: floatRegex },
      { name: "overlap2", displayName: "Overlap 2", type: "text", reg: floatRegex },
      { name: "overlap3", displayName: "Overlap 3", type: "text", reg: floatRegex },
      { name: "overlap4", displayName: "Overlap 4", type: "text", reg: floatRegex },
      { name: "overlap5", displayName: "Overlap 5", type: "text", reg: floatRegex },
      { name: "overlap6", displayName: "Overlap 6", type: "text", reg: floatRegex },
      { name: "overlap7", displayName: "Overlap 7", type: "text", reg: floatRegex },
      { name: "overlap8", displayName: "Overlap 8", type: "text", reg: floatRegex },
      //deblockedMetrage
    ]
  },

  cuttingRequestSerieRouleauHistory: {
    displayName: "Historique des Rouleaux",
    operation: [], // "Export",
    firstOrderProperty: "changeDate",

    fields: [
      { name: "id", displayName: "Id", type: "text", required: true, reg: /^[0-9]*$/ },
      { name: "changedBy", displayName: "Changed By", type: "text" },
      { name: "changeDate", displayName: "Change Date", type: "date" },
      { name: "serie", displayName: "Serie", type: "text" },
      { name: "content", displayName: "Content", type: "text" },
    ],
  },


  cuttingRequestBoxData: {
    displayName: "DB Box",
    operation: ["Add", "Edit"], // "Export",
    firstOrderProperty: "id",
    fields: [
      { name: "id", displayName: "Id", type: "text", required: true, reg: /^[0-9]*$/ },
      { name: "sequence", displayName: "Sequence", type: "text" },
      { name: "partNumber", displayName: "Part Number", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "item", displayName: "Item", type: "text" },
      { name: "wo", displayName: "Wo", type: "text" },
      { name: "woid", displayName: "Woid", type: "text" },
      { name: "qtyBox", displayName: "Qty Box", type: "text", reg: /^[0-9]*$/ },
      { name: "gammePrinted", displayName: "Gamme Printed", type: "text", reg: /^[0-9]*$/ },
    ],
    fieldsFilter: [
      { name: "id", displayName: "Id", type: "text", required: true, reg: /^[0-9]*$/ },
      { name: "sequence", displayName: "Sequence", type: "text" },
      { name: "partNumber", displayName: "Part Number", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "item", displayName: "Item", type: "text" },
      { name: "wo", displayName: "Wo", type: "text" },
      { name: "woid", displayName: "Woid", type: "text" },
      { name: "qtyBox", displayName: "Qty Box", type: "text", reg: /^[0-9]*$/ },
      { name: "gammePrinted", displayName: "Gamme Printed", type: "text", reg: /^[0-9]*$/ },

    ]
  },
  workOrder: {
    displayName: "Work Order",
    operation: [], // "Export",
    firstOrderProperty: "wo",
    fields: [
      { name: "wo", displayName: "WO", type: "text", required: true },
      { name: "woid", displayName: "WOID", type: "text", required: true },
      { name: "item", displayName: "Item", type: "text", required: true },
      { name: "partNumber", displayName: "Part Number", type: "text", required: true },
      { name: "description", displayName: "Description", type: "text", required: true },
      { name: "groupName", displayName: "Group Name", type: "text", required: true },
      { name: "designGroup", displayName: "Design Group", type: "text", required: true },
      { name: "coverGroup", displayName: "Cover Group", type: "text", required: true },
      { name: "partNumberStatus", displayName: "Part Number Status", type: "text", required: true },
      { name: "qtyOpen", displayName: "Qty Open", type: "text", required: true },
      { name: "qtyRejeter", displayName: "Qty Rejeter", type: "text", required: true },
      { name: "qtyCompleted", displayName: "Qty Completed", type: "text", required: true },
      { name: "dueDate", displayName: "Due Date", type: "date", required: true },
      { name: "shift", displayName: "Shift", type: "text", required: true },
      { name: "status", displayName: "Status", type: "text", required: true },
      { name: "createdAt", displayName: "Created At", type: "date", required: true },
      { name: "updatedAt", displayName: "Updated At", type: "date", required: true },
    ],
  },

  plieConfig: {
    displayName: "Pli Config",
    operation: ["Add", "Edit", "Delete"], // "Export",
    firstOrderProperty: "id",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: 'projet', displayName: 'Projet', type: "object", required: true, formDisplayProperty: "nom", formObject: "projet" },
      { name: "partNumberMaterial", displayName: "Part Number Material", type: "text" },
      { name: "plieOld", displayName: "Pli Old", type: "text", reg: /^[0-9]*$/ },
      { name: "plieNew", displayName: "Pli New", type: "text", reg: /^[0-9]*$/ },
      { name: "rotation", displayName: "Rotation", type: "text" },

    ],
    fieldsFilter: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/ },
      { name: 'projet.nom', displayName: 'Projet', type: "text", required: true },
      { name: "plieOld", displayName: "Pli Old", type: "text", reg: /^[0-9]*$/ },
      { name: "plieNew", displayName: "Pli New", type: "text", reg: /^[0-9]*$/ },
    ]
  },

  placement: {
    displayName: "Placement",
    operation: [], // "Export",
    firstOrderProperty: "updatedAt",
    fields: [
      { name: "placement", displayName: "Placement", type: "text" },
      { name: "folder", displayName: "Folder", type: "text" },
      { name: "partNumberMaterial", displayName: "Part Number Material", type: "text" },
      { name: "longueur", displayName: "Longueur", type: "text" },
      { name: "largeur", displayName: "Largeur", type: "text" },
      { name: "efficience", displayName: "Efficience", type: "text" },
      { name: "nbrPieces", displayName: "Nbr Pieces", type: "text" },
      { name: "updatedAt", displayName: "Updated At", type: "date" },
    ]
  },
  placementDetail: {
    displayName: "Placement Detail",
    operation: [], // "Export",
    firstOrderProperty: "updatedAt",
    fields: [
      { name: "placement", displayName: "Placement", type: "text" },
      { name: "folder", displayName: "Folder", type: "text" },
      { name: "ind", displayName: "ind", type: "text" },
      { name: "pattern", displayName: "pattern", type: "text" },
      { name: "idPaquet", displayName: "idPaquet", type: "text" },
      { name: "nomMedele", displayName: "nomMedele", type: "text" },
      { name: "gaucheDroite", displayName: "gaucheDroite", type: "text" },
      { name: "updatedAt", displayName: "Updated At", type: "date" },
    ]
  },
  plsProjet: {
    displayName: "Projet PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "nom", displayName: "Nom", type: "text", required: true },
    ],
  },
  plsDemande: {
    displayName: "PLS Demande",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "chefEquipe", displayName: "Chef Equipe", type: "text" },
      { name: "lieuDetection", displayName: "Lieue Detection", type: "text" },
      { name: "chaine", displayName: "Chaine", type: "text" },
      { name: "projet", displayName: "Projet", type: "object", formObject: "plsProjet", formDisplayProperty: "nom" },
      { name: "site", displayName: "Site", type: "text" },
      { name: "typeDefaut", displayName: "Type Defaut", type: "text" },
      { name: "typeDemande", displayName: "Type Demande", type: "text" },
      { name: "defaut", displayName: "Defaut", type: "text" },
      { name: "envoyerCAD", displayName: "Envoyer CAD", type: "boolean" },
      { name: "createdBy", displayName: "Créer Par", type: "text" },
      { name: "createdAt", displayName: "Créer Le", type: "text" },
      { name: "active", displayName: "Active", type: "boolean" },
      { name: "traiterBy", displayName: "Traiter Par", type: "text" },
      { name: "traiterAt", displayName: "Traiter Le", type: "text" },
      { name: "commentaire", displayName: "Commentaire", type: "text" },
      { name: "qualityCommentaire", displayName: "Quality Commentaire", type: "text" },
      { name: "reponse", displayName: "Reponse", type: "text" },
      { name: "validerAt", displayName: "Valider Le", type: "text" },
      { name: "validerBy", displayName: "Valider Par", type: "text" },
      { name: "valider", displayName: "Valider", type: "text" },
      { name: "responsableEmail", displayName: "Responsable Email", type: "text" },
      { name: "waitCAD", displayName: "Wait CAD", type: "option", optionsList: [...optionStatus] },
      { name: "waitVariance", displayName: "Wait Variance", type: "option", optionsList: [...optionStatus] },
      { name: "waitRecut", displayName: "Wait Recut", type: "option", optionsList: [...optionStatus] },
      { name: "userCAD", displayName: "User CAD", type: "text" },
      { name: "dateCAD", displayName: "Date CAD", type: "text" },
      { name: "userVariance", displayName: "User Variance", type: "text" },
      { name: "dateVariance", displayName: "Date Variance", type: "text" },
      { name: "userRecut", displayName: "User Recut", type: "text" },
      { name: "dateRecut", displayName: "Date Recut", type: "text" },
      { name: "waitMatelassage", displayName: "Wait Matelassage", type: "option", optionsList: [...optionStatus] },
      { name: "waitProd", displayName: "Wait Prod", type: "option", optionsList: [...optionStatus] },
      { name: "tableProd", displayName: "Table Prod", type: "text" },
      { name: "tableProdTime", displayName: "Table Prod Time", type: "text" },
      { name: "tableProdUser", displayName: "Table Prod User", type: "text" },
      { name: "userTransport", displayName: "User Transport", type: "text" },
      { name: "dateTransport", displayName: "Date Transport", type: "text" },
      { name: "plsFile", displayName: "Pls File", type: "text" },
      { name: "qualityFile", displayName: "Quality File", type: "text" },
      { name: "cadBlock", displayName: "Cad Block", type: "boolean" },
      { name: "cadBlockReason", displayName: "Cad Block Reason", type: "text" },
      { name: "cloturer", displayName: "Cloturer", type: "boolean" },
      { name: "cloturerBy", displayName: "Cloturer By", type: "text" },
      { name: "cloturerDate", displayName: "Cloturer Date", type: "text" },
      { name: "errorMessage", displayName: "Error Message", type: "text" },
    ],
  },
  plsSubDemande: {
    displayName: "PLS Sub Demande",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "demande",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "demande", displayName: "Demande", type: "text" },
      { name: "sequence", displayName: "Sequence", type: "text" },
      { name: "pn", displayName: "Pn", type: "text" },
      { name: "partNumberCoverDesciption", displayName: "Part Number Cover Desciption", type: "text" },
      { name: "empNumb", displayName: "Emp Numb", type: "text" },
      { name: "quantite", displayName: "Quantite", type: "text" },
      { name: "poste", displayName: "Poste", type: "text" },
      { name: "matricule", displayName: "Matricule", type: "text" },
      { name: "reponse", displayName: "Reponse", type: "text" },
      { name: "pnEmp", displayName: "Pn Emp", type: "text" },
      { name: "partNumberMaterial", displayName: "Part Number Material", type: "text" },
      { name: "partNumberMaterialDescription", displayName: "Part Number Material Description", type: "text" },
      { name: "placement", displayName: "Placement", type: "text" },
      { name: "config", displayName: "Config", type: "text" },
      { name: "laLaizeDemande", displayName: "La Laize Demande", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "drill1", displayName: "Drill1", type: "text" },
      { name: "drill2", displayName: "Drill2", type: "text" },
      { name: "sens", displayName: "Sens", type: "text" },
      { name: "longueurPlacement", displayName: "Longueur Placement", type: "text" },
      { name: "longueurMatelas", displayName: "Longueur Matelas", type: "text" },
      { name: "stock", displayName: "Stock", type: "text" },
      { name: "longueur", displayName: "Longueur", type: "text" },
      { name: "largeur", displayName: "Largeur", type: "text" },
      { name: "total", displayName: "Total", type: "text" },
      { name: "resteRouleau", displayName: "Reste Rouleau", type: "text" },
      { name: "demandeVariance", displayName: "Demande Variance", type: "text" },
      { name: "zoneRouleau", displayName: "Zone Rouleau", type: "text" },
      { name: "placementEmp", displayName: "Placement Emp", type: "text" },
      { name: "transport", displayName: "Transport", type: "text" },
      { name: "printed", displayName: "Printed", type: "boolean" },
      { name: "nbrCouche", displayName: "Nbr Couche", type: "text" },
      { name: "nlotfrs", displayName: "Nlotfrs", type: "text" },
    ],
  },

  /*
plsProdTicket: 
  private Long id;
  @Column(name= "pls_id")
  private String plsId;
	
  private String reftissu;
	
  private String description;
  @Column(name= "labelId")
  private String labelId;
	
  private Double quantity;
  @Column(name= "lotNr")
  private String lotNr;
  @Column(name= "tableName")
  private String tableName;
  @Column(name= "initQuantity")
  private Double initQuantity;
  @Column(name= "prixUnit")
  private Double prixUnit;
  @Column(name= "prixTotal")
  private Double prixTotal;
	
  @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    @Column(updatable = false)
  private LocalDateTime createdAt;
  @Column(name= "quantitePLS")
  private Double quantitePLS;

  */

  plsProdTicket: {
    displayName: "Rapport PLS",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "plsId", displayName: "PLS Id", type: "text" },
      { name: "reftissu", displayName: "Reftissu", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "labelId", displayName: "Label Id", type: "text" },
      { name: "quantity", displayName: "Quantity", type: "text" },
      { name: "lotNr", displayName: "Lot Nr", type: "text" },
      { name: "tableName", displayName: "Table Name", type: "text" },
      { name: "initQuantity", displayName: "Init Quantity", type: "text" },
      { name: "prixUnit", displayName: "Prix Unit", type: "text" },
      { name: "prixTotal", displayName: "Prix Total", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text" },
      { name: "quantitePLS", displayName: "Quantite PLS", type: "text" },
    ],
  },

  plsDemandeHistory: {
    displayName: "PLS Demande History",
    operation: [],
    firstOrderProperty: "updatedAt",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "updatedBy", displayName: "Updated By", type: "text" },
      { name: "updatedAt", displayName: "Updated At", type: "text" },
      { name: "typeOperation", displayName: "Type Operation", type: "text" },
      { name: "table", displayName: "Table", type: "text" },
      { name: "changement", displayName: "Changement", type: "text" },
    ],
  },

  plsSite: {
    displayName: "Site PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "nom", displayName: "Nom", type: "text", required: true },
    ],
  },

  plsLieuDetection: {
    displayName: "Lieu Detection PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "nom", displayName: "Nom", type: "text", required: true },
    ],
  },

  plsChaine: {
    displayName: "Chaine PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "nom", displayName: "Nom", type: "text", required: true },
    ],
  },

  plsDefaut: {
    displayName: "Defaut PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'code', displayName: 'Code', type: "text", required: true },
      { name: "description", displayName: "Description", type: "text", required: true },
      { name: "responsable", displayName: "Responsable", type: "text" },
      { name: "typeDefaut", displayName: "Type Defaut", type: "text" },
      { name: "active", displayName: "Active", type: "boolean" },
    ],
  },

  plsScrap: {
    displayName: "Scrap PLS",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "chefEquipe", displayName: "Chef Equipe", type: "text" },
      { name: "lieuDetection", displayName: "Lieu Detection", type: "object", formObject: "plsLieuDetection", formDisplayProperty: "nom" },
      { name: "chaine", displayName: "Chaine", type: "object", formObject: "plsChaine", formDisplayProperty: "nom" },
      { name: "projet", displayName: "Projet", type: "object", formObject: "plsProjet", formDisplayProperty: "nom" },
      { name: "site", displayName: "Site", type: "object", formObject: "plsSite", formDisplayProperty: "nom" },
      { name: "typeDefaut", displayName: "Type Defaut", type: "object", formObject: "plsDefaut", formDisplayProperty: "description" },
      { name: "defaut", displayName: "Defaut", type: "object", formObject: "plsDefaut", formDisplayProperty: "description" },
      { name: "reponse", displayName: "Réponse", type: "text" },
      { name: "commentaire", displayName: "Commentaire", type: "text" },
      { name: "createdAt", displayName: "Créer Le", type: "text" },
      { name: "active", displayName: "Active", type: "boolean" },
      { name: "cloturer", displayName: "Clôturé", type: "boolean" },
    ],
  },

  plsSubScrap: {
    displayName: "Sub Scrap PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "scrap", displayName: "scrap", type: "text" },
      { name: "wo", displayName: "WO", type: "text" },
      { name: "pn", displayName: "PN", type: "text" },
      { name: "quantite", displayName: "Quantité", type: "text" },
      { name: "poste", displayName: "Poste", type: "text" },
      { name: "matricule", displayName: "Matricule", type: "text" },
      { name: "reponse", displayName: "Réponse", type: "text" },
      { name: "price", displayName: "Prix", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "causeScrap", displayName: "Cause Scrap", type: "object", formObject: "plsCauseScrap", formDisplayProperty: "titre" },
      { name: "chefEquipe", displayName: "Chef Equipe", type: "text" },
    ],
  },

  plsScrapRapport: {
    displayName: "Scrap Rapport PLS",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "scrap", displayName: "scrap", type: "text" },
      { name: "pn", displayName: "PN", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "quantiteScrap", displayName: "Quantité Scrap", type: "text" },
      { name: "prixUnit", displayName: "Prix Unit", type: "text" },
      { name: "prixTotal", displayName: "Prix Total", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text" },
    ],
  },

  plsCauseScrap: {
    displayName: "Cause Scrap PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "titre", displayName: "Titre", type: "text" },
      { name: "active", displayName: "Active", type: "boolean" },
    ],
  },

  plsMachine: {
    displayName: "Machine PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "nom", displayName: "Nom", type: "text", required: true },
    ],
  },

  plsRapportRestRouleau: {
    displayName: "Rapport Rest Rouleau PLS",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "reftissu", displayName: "Ref Tissu", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "quantitePLS", displayName: "Quantite PLS", type: "text" },
      { name: "prixUnit", displayName: "Prix Unit", type: "text" },
      { name: "prixTotal", displayName: "Prix Total", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text" },
    ],
  },

  plsReftissu: {
    displayName: "Ref Tissu PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true },
      { name: "oldReftissu", displayName: "Old Ref Tissu", type: "text", required: true },
    ],
  },

  plsReftissuAlert: {
    displayName: "Ref Tissu Alert PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true },
      { name: "alertContent", displayName: "Alert Content", type: "text" },
    ],
  },

  plsAirbagDetail: {
    displayName: "Airbag Detail PLS",
    operation: ["Add", "Edit", "Delete"],
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "subDemandeId", displayName: "Sub Demande ID", type: "text" },
      { name: "partNumberMaterial", displayName: "Part Number Material", type: "text" },
      { name: "type", displayName: "Type", type: "text", required: true },
      { name: "ref", displayName: "Ref", type: "text", required: true },
      { name: "quantite", displayName: "Quantité", type: "text" },
    ],
  },


  spliceMarker: {
    displayName: "Marker",
    operation: [],
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "marker", displayName: "Marker", type: "text" },
      { name: "fabricType", displayName: "Fabric Type", type: "text" },
      { name: "markerLengthNet", displayName: "Marker Length Net", type: "text" },
      { name: "markerLengthBrut", displayName: "Marker Length Brut", type: "text" },
      { name: "numberOfLayers", displayName: "Number Of Layers", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text" },
      { name: "updatedAt", displayName: "Updated At", type: "text" },
      { name: "oldSpliceLength", displayName: "Old Splice Length", type: "text" },
      { name: "drill1", displayName: "Drill1", type: "text" },
      { name: "drill2", displayName: "Drill2", type: "text" },
      { name: "numberOfSets", displayName: "Number Of Sets", type: "text" },
      { name: "markerWidthBrut", displayName: "Marker Width Brut", type: "text" },
      { name: "fabricTypeDescription", displayName: "Fabric Type Description", type: "text" },
      { name: "numberOfPiecePerLayer", displayName: "Number Of Piece Per Layer", type: "text" },
      { name: "checkMaterial", displayName: "Check Material", type: "text" },
    ],
  },
  spliceMarkerLog: {
    displayName: "Marker Log",
    operation: ["Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: 'id', displayName: 'id', type: "text", required: true, hideForm: true },
      { name: "marker", displayName: "Marker", type: "text" },
      { name: "fabricType", displayName: "Fabric Type", type: "text" },
      { name: "markerLengthNet", displayName: "Marker Length Net", type: "text" },
      { name: "markerLengthBrut", displayName: "Marker Length Brut", type: "text" },
      { name: "fabricTypeSpliceLength", displayName: "Fabric Type Splice Length", type: "text" },
      { name: "fabricTypeDefectLength", displayName: "Fabric Type Defect Length", type: "text" },
      { name: "spliceNumber", displayName: "Splice Number", type: "text" },
      { name: "combined", displayName: "Combined", type: "text" },
      { name: "numberOfLayersToDo", displayName: "Number Of Layers To Do", type: "text" },
      { name: "numberOfLayersDone", displayName: "Number Of Layers Done", type: "text" },
      { name: "fabricTypeLength", displayName: "Fabric Type Length", type: "text" },
      { name: "state", displayName: "State", type: "text" },
      { name: "userBadge", displayName: "User Badge", type: "text" },
      { name: "userName", displayName: "User Name", type: "text" },
      { name: "stationNumber", displayName: "Station Number", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text" },
      { name: "updatedAt", displayName: "Updated At", type: "text" },
      { name: "sync", displayName: "Sync", type: "text" },
      { name: "buffer", displayName: "Buffer", type: "text" },
      { name: "orderCode", displayName: "Order Code", type: "text" },
      { name: "endOfRollLength", displayName: "End Of Roll Length", type: "text" },
      { name: "workOrderCode", displayName: "Work Order Code", type: "text" },
      { name: "framingMarker", displayName: "Framing Marker", type: "text" },
      { name: "fabricTypeGoodPartLength", displayName: "Fabric Type Good Part Length", type: "text" },
      { name: "numberOfSets", displayName: "Number Of Sets", type: "text" },
      { name: "numberOfInvertSplice", displayName: "Number Of Invert Splice", type: "text" },
      { name: "materialRollWidth", displayName: "Material Roll Width", type: "text" },
      { name: "markerWidthBrut", displayName: "Marker Width Brut", type: "text" },
      { name: "fabricTypeSpliceLengthInversed", displayName: "Fabric Type Splice Length Inversed", type: "text" },
      { name: "longestPieceLength", displayName: "Longest Piece Length", type: "text" },
      { name: "scannedMarker", displayName: "Scanned Marker", type: "text" },
      { name: "mrkDrill1", displayName: "Mrk Drill1", type: "text" },
      { name: "mrkDrill2", displayName: "Mrk Drill2", type: "text" },
      { name: "startMarginLength", displayName: "Start Margin Length", type: "text" },
      { name: "stopMarginLength", displayName: "Stop Margin Length", type: "text" },
      { name: "initialStartMarginLength", displayName: "Initial Start Margin Length", type: "text" },
      { name: "initialStopMarginLength", displayName: "Initial Stop Margin Length", type: "text" },
      { name: "fabricTypeDescription", displayName: "Fabric Type Description", type: "text" },
      { name: "fabricTypeCategory", displayName: "Fabric Type Category", type: "text" },
      { name: "confirmDenyCuttingBadge", displayName: "Confirm Deny Cutting Badge", type: "text" },
      { name: "confirmDenyCuttingName", displayName: "Confirm Deny Cutting Name", type: "text" },
      { name: "manualMode", displayName: "Manual Mode", type: "text" },
      { name: "standardTimePerLayer", displayName: "Standard Time Per Layer", type: "text" },
      { name: "standardTimePerRoll", displayName: "Standard Time Per Roll", type: "text" },
      { name: "breakTime", displayName: "Break Time", type: "text" },
      { name: "criticalItemApprovedByBadge", displayName: "Critical Item Approved By Badge", type: "text" },
      { name: "criticalItemApprovedByName", displayName: "Critical Item Approved By Name", type: "text" },

    ],
  },
  /*
    private Long id;
    @Column(name = "station_number")
    private String stationNumber;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "left_sensor_value")
    private String leftSensorValue;
    @Column(name = "right_sensor_value")
    private String rightSensorValue;
    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime updatedAt;
    @Column(name = "sync")
    private String sync;

  */
  calibrationLog: {
    displayName: "Calibration Log",
    operation: [],
    firstOrderProperty: "createdAt",
    fields: [
      { name: "id", displayName: "Id", type: "text" },
      { name: "stationNumber", displayName: "Station Number", type: "text" },
      { name: "userName", displayName: "User Name", type: "text" },
      { name: "leftSensorValue", displayName: "Left Sensor Value", type: "text" },
      { name: "rightSensorValue", displayName: "Right Sensor Value", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text" },
      { name: "updatedAt", displayName: "Updated At", type: "text" },
      { name: "sync", displayName: "Sync", type: "text" },
    ],
  },
  coupeMachineHistory: {
    displayName: "Coupe Machine History",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "lineDate",
    fields: [
      { name: 'machine', displayName: 'Machine', type: "text", required: true, hideForm: true },
      { name: 'fileReport', displayName: 'File Report', type: "text", required: true, hideForm: true },
      { name: 'ind', displayName: 'Ind', type: "text", required: true, hideForm: true },
      { name: "lineDate", displayName: "Line Date", type: "text" },
      { name: "placement", displayName: "Placement", type: "text" },
      { name: "errorCode", displayName: "Error Code", type: "text" },
      { name: "type", displayName: "Type", type: "text" },
      { name: "extra", displayName: "Extra", type: "text" },
    ],
  },
  /*
      @Id
    private String machine;
    @Id
    private String placement;
    @Id
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    private LocalDate date;
    private String shift;

    private String serie;
    private Double compteur;
    private Double nbrPieces;

    private Double running;
    private Double interruption;
    private Double reperage;
    private Double reperagePlus;
    private Double horsCycle;

    private String coupeur;
  */
  coupePerformance: {
    displayName: "Coupe Performance",
    operation: [],
    firstOrderProperty: "dateDebut",
    fields: [
      { name: 'machine', displayName: 'Machine', type: "text", required: true },
      { name: 'placement', displayName: 'Placement', type: "text", required: true },
      { name: 'dateDebut', displayName: 'Date Debut', type: "text", required: true },
      { name: 'dateFin', displayName: 'Date Fin', type: "text", required: true },
      { name: 'date', displayName: 'Date', type: "text" },
      { name: 'shift', displayName: 'Shift', type: "text" },
      { name: 'serie', displayName: 'Serie', type: "text" },
      { name: 'compteur', displayName: 'Compteur', type: "text" },
      { name: 'nbrPieces', displayName: 'Nbr Pieces', type: "text" },
      { name: 'running', displayName: 'Running', type: "text" },
      { name: 'interruption', displayName: 'Interruption', type: "text" },
      { name: 'reperage', displayName: 'Reperage', type: "text" },
      { name: 'reperagePlus', displayName: 'Reperage Plus', type: "text" },
      { name: 'horsCycle', displayName: 'Hors Cycle', type: "text" },
      { name: 'coupeur', displayName: 'Coupeur', type: "text" },
    ],
  },
  cuttingSpeed: {
    displayName: "Vitesse de coupe",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "config",
    fields: [
      { name: 'config', displayName: 'config', type: "text", required: true },
      { name: "vitesse", displayName: "Vitesse", type: "text" },
    ],
  },
  drillEmp: {
    displayName: "Drill Emp",
    operation: ["Add", "Edit", "Delete", "Import"],
    firstOrderProperty: "updateAt",
    fields: [
      { name: 'pattern', displayName: 'pattern', type: "text", required: true },
      { name: 'projet', displayName: 'Projet', type: "text", required: true },
      { name: "drill1", displayName: "Drill1", type: "text" },
      { name: "drill2", displayName: "Drill2", type: "text" },
      { name: "updateAt", displayName: "Update At", type: "text", hideForm: true },
    ],
  },
  /*
public class FirstCheckConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
	
  private String category;
  private Integer taskNumber;
  private String task;
  private String taskImage;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  */
  firstCheckConfig: {
    displayName: "Maitenance 1er niveau Config",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "category", displayName: "Category", type: "option", optionsList: [...optionCategory], required: false },
      { name: "type", displayName: "Type", type: "option", optionsList: [...optionTypes], required: false },

      { name: "taskNumber", displayName: "Task Number", type: "text" },
      { name: "task", displayName: "Task", type: "text" },
      { name: "taskDescription", displayName: "Task Description", type: "text" },

      { name: "taskImage", displayName: "Task Image", type: "image" },
      { name: "createdAt", displayName: "Created At", type: "text", hideForm: true },
      { name: "updatedAt", displayName: "Updated At", type: "text", hideForm: true },
    ],
  },

  firstCheck: {
    displayName: "Maitenance 1er niveau",
    operation: [],
    firstOrderProperty: "createdAt",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: 'date', displayName: 'Date', type: "text", required: true },
      { name: "shift", displayName: "Shift", type: "text" },
      { name: "machine", displayName: "Machine", type: "text" },
      { name: "category", displayName: "Category", type: "option", optionsList: [...optionCategory], required: false },
      { name: "taskNumber", displayName: "Task Number", type: "text" },
      { name: "task", displayName: "Task", type: "text" },
      { name: "taskDescription", displayName: "Task Description", type: "text" },
      { name: "taskImage", displayName: "Task Image", type: "image" },
      { name: "decision", displayName: "Decision", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text", hideForm: true },
    ],
  },

  auditQualiteConfig: {
    displayName: "Ok Démarrage Qualité Config",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "taskNumber", displayName: "Task Number", type: "text" },
      { name: "task", displayName: "Task", type: "text" },
      { name: "taskDescription", displayName: "Task Description", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text", hideForm: true },
      { name: "updatedAt", displayName: "Updated At", type: "text", hideForm: true },
    ],
  },

  auditQualite: {
    displayName: "Ok Démarrage Qualité",
    operation: [],
    firstOrderProperty: "createdAt",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: 'date', displayName: 'Date', type: "text", required: true },
      { name: "shift", displayName: "Shift", type: "text" },
      { name: "tableControle", displayName: "Table Controle", type: "text" },
      { name: "taskNumber", displayName: "Task Number", type: "text" },
      { name: "task", displayName: "Task", type: "text" },
      { name: "taskDescription", displayName: "Task Description", type: "text" },
      { name: "decision", displayName: "Decision", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text", hideForm: true },
      { name: "createdBy", displayName: "Created By", type: "text", hideForm: true },
    ],
  },
  machineDxfRapport: {
    displayName: "Rapport DXF",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "startTime",
    fields: [
      { name: "processID", displayName: "Process ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "userName", displayName: "User Name", type: "text" },
      { name: "machineName", displayName: "Machine Name", type: "text" },
      { name: "device", displayName: "Device", type: "text" },
      { name: "job", displayName: "Job", type: "text" },
      { name: "startTime", displayName: "Start Time", type: "text" },
      { name: "endTime", displayName: "End Time", type: "text" },
      { name: "material", displayName: "Material", type: "text" },
      { name: "matConsumption", displayName: "Mat Consumption", type: "text" },
      { name: "matConsumptionArea", displayName: "Mat Consumption Area", type: "text" },
      { name: "cutPathLength", displayName: "Cut Path Length", type: "text" },
      { name: "cuttingTimeInSecs", displayName: "Cutting Time In Secs", type: "text" },
      { name: "pTime", displayName: "P Time", type: "text" },
      { name: "movementLength", displayName: "Movement Length", type: "text" },
      { name: "totalCount", displayName: "Total Count", type: "text" },
      { name: "count", displayName: "Count", type: "text" },
      { name: "segments", displayName: "Segments", type: "text" },
      { name: "state", displayName: "State", type: "text" },
    ],
  },
  machineLsrRapport: {
    displayName: "Rapport LSR",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "date",
    fields: [
      { name: "machine", displayName: "Machine", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "date", displayName: "Date", type: "text" },
      { name: "label", displayName: "Label", type: "text" },
      { name: "value", displayName: "Value", type: "text" },
    ],
  },
  maintenanceInterventionConfig: {
    displayName: "Maintenance Machine Config",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "serviceType", displayName: "Service Type", type: "text" },
      { name: 'machineType', displayName: 'Type De Machine', type: "object", required: true, formDisplayProperty: "name", formObject: "machineType" },
      { name: "serviceDescription", displayName: "Service Description", type: "text" },
      { name: "frequency", displayName: "Frequency", type: "text" },
      { name: "active", displayName: "Active", type: "boolean" },
      { name: "notificationPercentage", displayName: "Notification Percentage", type: "text" },
    ],
  },
  maintenanceIntervention: {
    displayName: "Maintenance Machine",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "date",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "machine", displayName: "Machine", type: "text" },
      { name: "date", displayName: "Date", type: "text" },
      { name: "userName", displayName: "User Name", type: "text" },
      { name: "service", displayName: "Service", type: "text" },
      { name: "serviceType", displayName: "Service Type", type: "text" },
      { name: "counter", displayName: "Counter", type: "text" },
    ],
  },


  /*
  partNumberCorrespendance : 
      @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partNumber;

    private String partNumberCorrespondance;

    private String placement;
*/
  partNumberCorrespendance: {
    displayName: "Part Number Correspendance",
    operation: ["Add", "Edit", "Delete", "Import"],
    firstOrderProperty: "partNumber",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "partNumber", displayName: "Part Number", type: "text" },
      { name: "partNumberCorrespondance", displayName: "Part Number Correspondance", type: "text" },
      // private String pattern;
      // private String patternCorrespondance;
      { name: "pattern", displayName: "Pattern", type: "text" },
      { name: "patternCorrespondance", displayName: "Pattern Correspondance", type: "text" },
      { name: "placement", displayName: "Placement", type: "text" },
    ],
  },
  reftissuProperty: {
    displayName: "Reftissu Properiété",
    operation: ["Add", "Delete", "Import"],
    firstOrderProperty: "reftissu",
    fields: [
      { name: "reftissu", displayName: "Reftissu", type: "text", required: true },
      { name: "property", displayName: "Property (Biais/Sens/...)", type: "text", required: true },
      { name: "value", displayName: "Value", type: "text" },
      { name: "value2", displayName: "Value 2", type: "text" },
    ],
  },

  consumable: {
    displayName: "Consumable",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "date",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, reg: /^[0-9]*$/, hideForm: true },
      { name: "machine", displayName: "Machine", type: "text" },
      { name: "date", displayName: "Date", type: "text" },
      { name: "name", displayName: "Name", type: "text" },
      { name: "type", displayName: "Type", type: "text" },
      { name: "count", displayName: "Count", type: "text" },
      { name: "value", displayName: "Value", type: "text" },
      { name: "unit", displayName: "Unit", type: "text" },
      { name: "mobileSet", displayName: "Mobile Set", type: "text" },
      { name: "isBroken", displayName: "Is Broken", type: "boolean" },
      { name: "firstDate", displayName: "First Date", type: "text" },
      { name: "mountingCount", displayName: "Mounting Count", type: "text" },
      { name: "valueName1", displayName: "Value Name 1", type: "text" },
      { name: "value1", displayName: "Value 1", type: "text" },
      { name: "unit1", displayName: "Unit 1", type: "text" },
      { name: "valueName2", displayName: "Value Name 2", type: "text" },
      { name: "value2", displayName: "Value 2", type: "text" },
      { name: "unit2", displayName: "Unit 2", type: "text" },
      { name: "valueName3", displayName: "Value Name 3", type: "text" },
      { name: "value3", displayName: "Value 3", type: "text" },
      { name: "unit3", displayName: "Unit 3", type: "text" },
      { name: "valueName4", displayName: "Value Name 4", type: "text" },
      { name: "value4", displayName: "Value 4", type: "text" },
      { name: "unit4", displayName: "Unit 4", type: "text" },
    ],
  },

  qualityNotice: {
    displayName: "Quality Notice",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "createdAt",
    tableSize: 100,
    fields: [
      { name: "numeroQn", displayName: "Numero Qn", type: "text", required: true, hideForm: true },
      { name: "ind", displayName: "Ind", type: "text" },
      { name: "coordinateur", displayName: "Chef direct", type: "text" },
      { name: "createdBy", displayName: "Created By", type: "text" },
      { name: "createdAt", displayName: "Created At", type: "text" },
      { name: "wo", displayName: "Wo", type: "text" },
      { name: "sequence", displayName: "Sequence", type: "text" },
      { name: "partnumber", displayName: "Partnumber", type: "text" },
      { name: "projet", displayName: "Projet", type: "text" },
      { name: 'site', displayName: 'Site', required: true, type: "option", optionsList: [...optionSite] },
      { name: "numEmp", displayName: "Num Emp", type: "text" },
      { name: "quantite", displayName: "Quantite", type: "text" },
      { name: "reftissu", displayName: "Reftissu", type: "text" },
      { name: "reftissuDescription", displayName: "reftissuDescription", type: "text" },
      { name: "nomFournisseur", displayName: "nomFournisseur", type: "text" },

      { name: "idRouleau", displayName: "idRouleau", type: "text" },
      { name: "lotFrs", displayName: "lotFrs", type: "text" },
      { name: "dateCoupe", displayName: "dateCoupe", type: "text" },

      { name: "typeDefaut", displayName: "Type Defaut", type: "option", optionsList: [...optionTypeDefaut] },
      { name: "codeDefaut", displayName: "Code Defaut", type: "object", formDisplayProperty: "code", formObject: "codeDefaut", required: true },
      { name: "description", displayName: "Description", type: "text", hideTable: true },

      { name: 'image1', displayName: 'Image1', type: "image", hideTable: true },
      { name: 'image2', displayName: 'Image2', type: "image", hideTable: true },

      { name: "correctDefaut", displayName: "Correct Defaut", type: "object", formDisplayProperty: "code", formObject: "codeDefaut", required: true },
      { name: "qteRecu", displayName: "Quantité reçue", type: "text" },
      { name: "qteRecuCoiffe", displayName: "Quantité reçue coiffes", type: "text" },

      { name: "machine", displayName: "Machine", type: "text" },
      { name: "traiterPar", displayName: "Traiter Par", type: "text" },
      { name: "dateTraitement", displayName: "Date Traitement", type: "text" },
      { name: "reponse", displayName: "Reponse", required: true, type: "option", optionsList: [...optionReponse] },
      {
        name: "decision", displayName: "Decision", required: true, type: "option", optionsList: [
          { label: "Scrap", value: "Scrap" }, { label: "Récupération", value: "Récupération" }, { label: "NA", value: "NA" }
        ]
      },
      { name: "securisation", displayName: "Securisation", type: "option", optionsList: [{ label: "NA", value: "NA" }, { label: "15 jours", value: "15 jours" }] },
      { name: "remarque", displayName: "Remarque / CAUSE POTENTIEL", type: "text", hideTable: true },
      { name: 'fichier', displayName: 'fichier', type: "file", hideTable: true },

      //sendNotificationDate
      { name: "sendNotificationDate", displayName: "Date Notification ", type: "text" },
      { name: "notificationBy", displayName: "Notification By", type: "text" },
      { name: "coupeValidationDate", displayName: "Date Validation Coupe", type: "text" },
      { name: "coupeValidationBY", displayName: "Validation Coupe Par", type: "text" },
      { name: "superviseurConfirmationDate", displayName: "Superviseur Confirmation Date", type: "text" },
      { name: "superviseurConfirmationBy", displayName: "Superviseur Confirmation Par", type: "text" },


    ],
  },

  demandeChangementSerie: {
    displayName: "Demande Changement Serie",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "dateCreation",
    tableSize: 100,
    fields: [
      { name: "id", displayName: "id", type: "text", hideForm: true },
      { name: "creePar", displayName: "creePar", type: "text", hideForm: true },
      { name: "dateCreation", displayName: "date", type: "text", hideForm: true },
      //typeDemande
      { name: "typeDemande", displayName: "Type Demande", type: "text", hideForm: true },
      { name: "reponseDepartement", displayName: "Réponse Département", type: "text", hideForm: true },
      { name: "departementValidation", displayName: "Département Validation", type: "text", hideForm: true },
      { name: "cause", displayName: "Cause", type: "text", hideForm: true },
      { name: "confirmeParDepartement", displayName: "Confirmé Par Département", type: "text", hideForm: true },
      { name: "dateConfirmationDepartement", displayName: "Date Confirmation Département", type: "text", hideForm: true },


      { name: "reponse", displayName: "CAD", type: "text", hideForm: true },
      { name: "confirmePar", displayName: "Confirmé Par", type: "text", hideForm: true },
      { name: "dateConfirmation", displayName: "Date Confirmation", type: "text", hideForm: true },

      { name: "serie", displayName: "Serie", type: "text" },
      { name: "sequence", displayName: "sequence", type: "text", hideForm: true },
      //projet
      { name: "projet", displayName: "Projet", type: "text", hideForm: true },
      { name: "partNumberMaterial", displayName: "partNumberMaterial", type: "text", hideForm: true },
      { name: "partNumbers", displayName: "Part Numbers", type: "text", hideForm: true },
      { name: "placement", displayName: "Placement", type: "text", hideForm: true },
      { name: 'machine', displayName: 'Type De Machine', type: "object", required: true, formDisplayProperty: "name", formObject: "machineType" },
      { name: "laize", displayName: "laize", type: "text" },
      //config
      { name: "config", displayName: "Config", type: "text" },
      { name: "autreChangement", displayName: "autreChangement", type: "textarea" },
      { name: "description", displayName: "description", type: "textarea" },
      { name: "statut", displayName: "statut", type: "text", hideForm: true },

      //cause

      { name: "newPlacement", displayName: "Nouveau Placement", type: "text", hideForm: true },

      //autreChangement

    ]
  },
  qualityValidationHistory: {
    displayName: "Quality Validation History",
    operation: [],
    firstOrderProperty: "date",
    fields: [
      { name: "serie", displayName: "Serie", type: "text", required: true, hideForm: true },
      { name: "reftissu", displayName: "Reftissu", type: "text" },
      { name: "date", displayName: "Date", type: "text" },
      { name: "machine", displayName: "Machine", type: "text" },
      { name: "matelasseur", displayName: "Matelasseur", type: "text" },
    ],
  },
  qualityValidationPattern: {
    displayName: "Quality Validation Pattern",
    operation: ["Add", "Edit", "Delete", "Search"],
    firstOrderProperty: "id",
    fields: [
      { name: "id", displayName: "id", type: "text", hideForm: true },
      { name: "machine", displayName: "Machine", type: "text", defaultValue: "" },
      { name: "placement", displayName: "Placement", type: "text", defaultValue: "" },
      { name: "partNumberMaterial", displayName: "Part Number Material", type: "text", defaultValue: "" },
      { name: "pattern", displayName: "Pattern", type: "text", defaultValue: "" },
      { name: "idRouleau", displayName: "ID Rouleau", type: "text", defaultValue: "" },
      { name: "lot", displayName: "Lot", type: "text", defaultValue: "" },
      {
        name: "applicationType", displayName: "Application Type", type: "option", optionsList: [
          { label: "Coupe", value: "coupe" },
          { label: "Matelassage", value: "matelassage" },
          { label: "BOTH", value: "BOTH" }
        ], defaultValue: ""
      },
      { name: "description", displayName: "Description", type: "text", defaultValue: "" },
      { name: "active", displayName: "Active", type: "boolean", defaultValue: true },
      { name: "createdAt", displayName: "Créé le", type: "datetime", hideForm: true },
      { name: "updatedAt", displayName: "Modifié le", type: "datetime", hideForm: true },
      { name: "createdBy", displayName: "Créé par", type: "text", hideForm: true },
      { name: "updatedBy", displayName: "Modifié par", type: "text", hideForm: true },
    ],
  },
  // ctcToleranceRule: {
  //   displayName: "CTC Tolerance Rule",
  //   operation: ["add", "edit", "delete", "search"],
  //   firstOrderProperty: "projet",
  //   fields: [
  //     { name: "projet", displayName: "Projet", type: "text" },
  //     { name: "type", displayName: "Type", type: "text" },
  //     { name: "heightMin", displayName: "Height Min", type: "text" },
  //     { name: "heightMax", displayName: "Height Max", type: "text" },
  //     { name: "toleranceMin1", displayName: "Tolerance Min1", type: "text" },
  //     { name: "toleranceMax1", displayName: "Tolerance Max1", type: "text" },
  //     { name: "toleranceMin2", displayName: "Tolerance Min2", type: "text" },
  //     { name: "toleranceMax2", displayName: "Tolerance Max2", type: "text" },
  //     { name: "toleranceDrill", displayName: "Tolerance Drill", type: "text" },
  //     { name: "priority", displayName: "Priority", type: "text" },
  //     { name: "active", displayName: "Active", type: "boolean" },
  //   ],
  // },

  qualityReftissuBlock: {
    displayName: "Quality Reftissu Block",
    operation: [],
    firstOrderProperty: "date",
    fields: [
      { name: "reftissu", displayName: "Reftissu", type: "text", required: true, hideForm: true },
      { name: "date", displayName: "Date", type: "text" },
      { name: "createdBy", displayName: "Created By", type: "text" },
    ],
  },

  configSeriePlus: {
    displayName: "Config Serie Plus",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "pattern",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, hideForm: true },
      { name: "pattern", displayName: "Pattern", type: "text" },
      { name: "partNumberMaterial", displayName: "Part Number Material", type: "text" },
      { name: "description", displayName: "Description", type: "text" },
      { name: "matelassageEndroit", displayName: "Matelassage Endroit", type: "text" },
      { name: "longueur", displayName: "Longueur", type: "text" },
      { name: "machine", displayName: "Machine", type: "text" },
      { name: "nbrCouche", displayName: "Nbr Couche", type: "text" },
      { name: "kits", displayName: "Kits", type: "text" },
      { name: "maxPlie", displayName: "Max Plie", type: "text" },
      { name: "placement", displayName: "Placement", type: "text" },
      { name: "laize", displayName: "Laize", type: "text" },
      { name: "config", displayName: "Config", type: "text" },
      { name: "drill", displayName: "Drill", type: "text" },
    ],
  },

  /*
  scanRouleau : 
      private String serialId;
    private String reftissu;
    private String quantite;
    private String emplacement;
    private String lot;

    private Double metrage;

    private Integer matricule;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

  */
  scanRouleau: {
    displayName: "Scan Rouleau",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "serialId",
    fields: [
      { name: "serialId", displayName: "Serial ID", type: "text", required: true },
      { name: "reftissu", displayName: "Reftissu", type: "text", required: true },
      { name: "quantite", displayName: "Quantité", type: "text", required: true },
      { name: "emplacement", displayName: "Emplacement", type: "text", required: true },
      { name: "lot", displayName: "Lot", type: "text", required: true },
      { name: "metrage", displayName: "Metrage", type: "text", required: true },
      { name: "matricule", displayName: "Matricule", type: "text", required: true },
      { name: "date", displayName: "Date", type: "text", required: true },
    ],
  },
  /*
    scanRouleauHistorique : 
        private Integer id;
  
  
      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
      private LocalDateTime date;
  
      private String serialId;
          private String content;
  
  
    */
  scanRouleauHistorique: {
    displayName: "Scan Rouleau Historique",
    operation: [],
    firstOrderProperty: "id",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true },
      { name: "date", displayName: "Date", type: "text", required: true },
      { name: "serialId", displayName: "Serial ID", type: "text", required: true },
      { name: "content", displayName: "Content", type: "text", required: true },
    ],
  },

  /*
public class ValidationQLaize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String itemNumber;
    private String um;
    private String abc;
    private String site;
    private LocalDate lastCnt;
    private String location;
    private String ref;
    private Double qtyOnHand;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validationDate;
    private String validatedBy;
    private String fournisseur;
    private Double laizeContractuel;
    private Double laizeReel;

}
  */
  validationQLaize: {
    displayName: "Validation QLaize",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "id",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true },
      { name: "itemNumber", displayName: "Item Number", type: "text", required: true },
      { name: "um", displayName: "UM", type: "text", required: true },
      { name: "abc", displayName: "ABC", type: "text", required: true },
      { name: "site", displayName: "Site", type: "text", required: true },
      { name: "lastCnt", displayName: "Last Count", type: "text", required: true },
      { name: "location", displayName: "Location", type: "text", required: true },
      { name: "ref", displayName: "Reference", type: "text", required: true },
      { name: "qtyOnHand", displayName: "Quantity on Hand", type: "text", required: true },
      { name: "status", displayName: "Status", type: "text", required: true },
      { name: "validationDate", displayName: "Validation Date", type: "text", required: true },
      { name: "validatedBy", displayName: "Validated By", type: "text", required: true },
      { name: "fournisseur", displayName: "Fournisseur", type: "text", required: true },
      { name: "laizeContractuel", displayName: "Laize Contractuel", type: "text", required: true },
      { name: "laizeReel", displayName: "Laize Reel", type: "text", required: true },
    ],
  },
  /*
 hardwareConfig
 public class HardwareConfig {
 
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;
     private String name;
     private String machine;
     private String type;
     private String link;
     private String rowNumber;
 }
  */
  hardwareConfig: {
    displayName: "Hardware Config",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "id",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true },
      { name: "name", displayName: "Name", type: "text", required: true },
      { name: "machine", displayName: "Machine", type: "text", required: true },
      { name: "type", displayName: "Type", type: "text", required: true },
      { name: "link", displayName: "Link", type: "text", required: true },
      { name: "rowNumber", displayName: "Row Number", type: "text", required: true },
    ],
  },

  // ==========================================
  // Scheduling Entities for OrdonnancementV2
  // ==========================================

  /*
    ShiftSchedule - Shift planning entity
  */
  shiftSchedule: {
    displayName: "Planning de Shift",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "shiftStart",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      {
        name: "shiftNumber", displayName: "Numéro de Shift", type: "option", required: true, optionsList: [
          { label: "Shift 1 (21:55-05:45)", value: 1 },
          { label: "Shift 2 (05:55-13:45)", value: 2 },
          { label: "Shift 3 (13:55-21:45)", value: 3 }
        ]
      },
      { name: "shiftStart", displayName: "Début de Shift", type: "datetime", required: true },
      { name: "shiftEnd", displayName: "Fin de Shift", type: "datetime", required: true },
      { name: "zone", displayName: "Zone", type: "object", required: true, formObject: "zone", formDisplayProperty: "nom" },
    ],
    fieldsFilter: [
      { name: "shiftNumber", displayName: "Numéro de Shift", type: "number" },
      { name: "zone.nom", displayName: "Zone", type: "text" },
    ],
  },

  /*
    MachineScheduleStatus - Machine availability status
  */
  machineScheduleStatus: {
    displayName: "Statut Machine Planification",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "machine",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "machine", displayName: "Machine", type: "object", required: true, formObject: "productionTable", formDisplayProperty: "nom" },
      { name: "available", displayName: "Disponible", type: "boolean", required: true },
      {
        name: "reason", displayName: "Raison", type: "option", optionsList: [
          { label: "PM (Maintenance Préventive)", value: "PM" },
          { label: "Maintenance", value: "maintenance" },
          { label: "Manque de personnel", value: "no_personnel" },
          { label: "Autre", value: "other" }
        ]
      },
      { name: "until", displayName: "Jusqu'à", type: "datetime" },
    ],
    fieldsFilter: [
      { name: "machine.nom", displayName: "Machine", type: "text" },
      { name: "available", displayName: "Disponible", type: "boolean" },
      { name: "reason", displayName: "Raison", type: "text" },
    ],
  },

  /*
    ScheduleInterval - Breaks and pauses
  */
  scheduleInterval: {
    displayName: "Intervalle de Planification",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "startTime",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "machine", displayName: "Machine (Global si vide)", type: "object", formObject: "productionTable", formDisplayProperty: "nom" },
      { name: "startTime", displayName: "Début", type: "datetime", required: true },
      { name: "endTime", displayName: "Fin", type: "datetime", required: true },
      {
        name: "type", displayName: "Type", type: "option", required: true, optionsList: [
          { label: "Pause", value: "pause" },
          { label: "Arrêt strict", value: "strict" }
        ]
      },
      { name: "description", displayName: "Description", type: "text" },
    ],
    fieldsFilter: [
      { name: "type", displayName: "Type", type: "text" },
      { name: "machine.nom", displayName: "Machine", type: "text" },
    ],
  },

  /*
    SequenceSchedule - Sequence planning
  */
  sequenceSchedule: {
    displayName: "Planification Séquence",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "priority",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "sequence", displayName: "Séquence", type: "object", required: true, formObject: "cuttingRequestData", formDisplayProperty: "sequence" },
      { name: "assignedZone", displayName: "Zone Assignée", type: "object", formObject: "zone", formDisplayProperty: "nom" },
      { name: "priority", displayName: "Priorité", type: "number" },
      {
        name: "status", displayName: "Statut", type: "option", optionsList: [
          { label: "Non démarré", value: "not_started" },
          { label: "En cours", value: "in_progress" },
          { label: "Terminé", value: "finished" }
        ]
      },
      { name: "excluded", displayName: "Exclue", type: "boolean" },
      { name: "estimatedCompletionTime", displayName: "Fin Estimée", type: "datetime" },
    ],
    fieldsFilter: [
      { name: "sequence.sequence", displayName: "Séquence", type: "text" },
      { name: "assignedZone.nom", displayName: "Zone", type: "text" },
      { name: "status", displayName: "Statut", type: "text" },
      { name: "excluded", displayName: "Exclue", type: "boolean" },
    ],
  },

  /*
    SerieSchedule - Serie planning with spreading and cutting details
  */
  serieSchedule: {
    displayName: "Planification Série",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "spreadingStartTime",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "serie", displayName: "Série", type: "object", required: true, formObject: "cuttingRequestSerieData", formDisplayProperty: "serie" },
      { name: "assignedSpreadingTable", displayName: "Table Matelassage", type: "object", formObject: "productionTable", formDisplayProperty: "nom" },
      { name: "spreadingStartTime", displayName: "Début Matelassage", type: "datetime" },
      { name: "spreadingEndTime", displayName: "Fin Matelassage", type: "datetime" },
      {
        name: "spreadingStatus", displayName: "Statut Matelassage", type: "option", optionsList: [
          { label: "En attente", value: "Waiting" },
          { label: "En cours", value: "In progress" },
          { label: "Terminé", value: "Complete" }
        ]
      },
      { name: "assignedCuttingMachine", displayName: "Machine Coupe", type: "object", formObject: "productionTable", formDisplayProperty: "nom" },
      { name: "cuttingStartTime", displayName: "Début Coupe", type: "datetime" },
      { name: "cuttingEndTime", displayName: "Fin Coupe", type: "datetime" },
      {
        name: "cuttingStatus", displayName: "Statut Coupe", type: "option", optionsList: [
          { label: "En attente", value: "Waiting" },
          { label: "En cours", value: "In progress" },
          { label: "Terminé", value: "Complete" }
        ]
      },
      { name: "estimatedSpreadingTime", displayName: "Temps Matelassage (min)", type: "number" },
      { name: "estimatedCuttingTime", displayName: "Temps Coupe (min)", type: "number" },
    ],
    fieldsFilter: [
      { name: "serie.serie", displayName: "Série", type: "text" },
      { name: "spreadingStatus", displayName: "Statut Matelassage", type: "text" },
      { name: "cuttingStatus", displayName: "Statut Coupe", type: "text" },
      { name: "assignedSpreadingTable.nom", displayName: "Table", type: "text" },
      { name: "assignedCuttingMachine.nom", displayName: "Machine", type: "text" },
    ],
  },

  /*
    MaterialLogistics - Material needs and availability
  */
  materialLogistics: {
    displayName: "Logistique Matériel",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "neededByTime",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "zone", displayName: "Zone", type: "object", required: true, formObject: "zone", formDisplayProperty: "nom" },
      { name: "partNumberMaterial", displayName: "Matériau (Part Number)", type: "text", required: true },
      { name: "requiredQuantity", displayName: "Quantité Requise (m)", type: "number", required: true },
      { name: "availableQuantity", displayName: "Quantité Disponible (m)", type: "number" },
      {
        name: "status", displayName: "Statut", type: "option", optionsList: [
          { label: "Nécessaire", value: "Needed" },
          { label: "Disponible", value: "Available" },
          { label: "Pénurie", value: "Shortage" }
        ]
      },
      { name: "neededByTime", displayName: "Besoin pour", type: "datetime" },
      { name: "assignedRoll", displayName: "Rouleau Assigné", type: "object", formObject: "scanRouleau", formDisplayProperty: "serialId" },
    ],
    fieldsFilter: [
      { name: "zone.nom", displayName: "Zone", type: "text" },
      { name: "partNumberMaterial", displayName: "Matériau", type: "text" },
      { name: "status", displayName: "Statut", type: "text" },
    ],
  },


  /*
    QualityPatternValidationHistory - Quality Pattern Validation History
  */
  qualityPatternValidationHistory: {
    displayName: "Historique Validation Empiècement",
    operation: [],
    firstOrderProperty: "validationDate",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "qualityValidationPattern.pattern", displayName: "Pattern", type: "text" },
      { name: "qualityValidationPattern.machine", displayName: "Machine", type: "text" },
      { name: "qualityValidationPattern.placement", displayName: "Placement", type: "text" },
      { name: "qualityValidationPattern.partNumberMaterial", displayName: "Part Number Material", type: "text" },
      { name: "serie", displayName: "Série", type: "text" },
      { name: "validatedBy", displayName: "Validé par", type: "text" },
      { name: "validationDate", displayName: "Date de validation", type: "datetime" },
      {
        name: "result", displayName: "Résultat", type: "option", optionsList: [
          { label: "OK", value: "OK" },
          { label: "NOK", value: "NOK" }
        ]
      },
      { name: "comment", displayName: "Commentaire", type: "textarea" },
    ],
    fieldsFilter: [
      { name: "qualityValidationPattern.pattern", displayName: "Pattern", type: "text" },
      { name: "qualityValidationPattern.machine", displayName: "Machine", type: "text" },
      { name: "serie", displayName: "Série", type: "text" },
      { name: "validatedBy", displayName: "Validé par", type: "text" },
      { name: "result", displayName: "Résultat", type: "text" },
    ],
  },

  /*
    CtcToleranceRule - CTC Tolerance Rules
  */
  ctcToleranceRule: {
    displayName: "Règles de Tolérance CTC",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "priority",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "projet", displayName: "Projet", type: "text" },
      {
        name: "type", displayName: "Type", type: "option", optionsList: [
          { label: "Tous", value: "" },
          { label: "Fabric", value: "fabric" },
          { label: "Supplier Kit Leather", value: "supplier kit leather" },
          { label: "Supplier Kit Fabric", value: "supplier kit fabric" },
          { label: "CNC", value: "CNC" }
        ]
      },
      {
        name: "laminateFilter", displayName: "Filtre Laminate", type: "option", optionsList: [
          { label: "Tous", value: "all" },
          { label: "Laminate uniquement (finit par L)", value: "laminate_only" },
          { label: "Non-laminate uniquement", value: "non_laminate_only" }
        ], defaultValue: "all"
      },
      {
        name: "applyOn", displayName: "Appliquer sur", type: "option", optionsList: [
          { label: "Max (hauteur/largeur)", value: "max" },
          { label: "Hauteur", value: "height" },
          { label: "Largeur", value: "width" }
        ], defaultValue: "max"
      },
      { name: "heightMin", displayName: "Dimension Min (mm)", type: "number", defaultValue: 0 },
      { name: "heightMax", displayName: "Dimension Max (mm)", type: "number" },
      { name: "toleranceMin1", displayName: "Tolérance Min 1", type: "number" },
      { name: "toleranceMax1", displayName: "Tolérance Max 1", type: "number" },
      { name: "toleranceMin2", displayName: "Tolérance Min 2", type: "number" },
      { name: "toleranceMax2", displayName: "Tolérance Max 2", type: "number" },
      { name: "toleranceDrill", displayName: "Tolérance Drill", type: "number" },
      { name: "priority", displayName: "Priorité", type: "number", defaultValue: 0 },
      { name: "active", displayName: "Actif", type: "boolean", defaultValue: true },
    ],
    fieldsFilter: [
      { name: "projet", displayName: "Projet", type: "text" },
      { name: "type", displayName: "Type", type: "text" },
      { name: "active", displayName: "Actif", type: "boolean" },
    ],
  },

  /*
    ScanXPL - XPL Scan Tracking (Read-Only)
  */
  scanXPL: {
    displayName: "Scan XPL",
    operation: [], // Read-only, no operations
    firstOrderProperty: "scanDate",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "serie", displayName: "Série", type: "text" },
      { name: "machine", displayName: "Machine", type: "text" },
      { name: "operator", displayName: "Opérateur", type: "text" },
      { name: "placement", displayName: "Placement", type: "text" },
      { name: "scanDate", displayName: "Date Scan", type: "datetime" },
      { name: "isFirstScan", displayName: "Premier Scan", type: "boolean" },
      { name: "autoUpdatePerformed", displayName: "Auto Update", type: "boolean" },
      { name: "createdAt", displayName: "Créé le", type: "datetime" },
    ],
    fieldsFilter: [
      { name: "serie", displayName: "Série", type: "text" },
      { name: "machine", displayName: "Machine", type: "text" },
      { name: "operator", displayName: "Opérateur", type: "text" },
      { name: "placement", displayName: "Placement", type: "text" },
      { name: "isFirstScan", displayName: "Premier Scan", type: "boolean" },
    ],
  },

  /*
    BoxWeight - Box Weight Tracking System
  */
  boxWeight: {
    displayName: "Poids Boîte",
    operation: ["search"], // Admin can search, specialized pages for fill/verify
    firstOrderProperty: "sentAt",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      {
        name: "boxType", displayName: "Type Boîte", type: "option", optionsList: [
          { label: "Grise", value: "gray" },
          { label: "Noire", value: "black" }
        ]
      },
      { name: "boxId", displayName: "ID Boîte", type: "text" },
      { name: "sentWeight", displayName: "Poids Envoyé (kg)", type: "number" },
      { name: "sentBy", displayName: "Envoyé par", type: "text", hideForm: true },
      { name: "sentAt", displayName: "Date Envoi", type: "datetime", hideForm: true },
      { name: "receivedWeight", displayName: "Poids Reçu (kg)", type: "number", hideForm: true },
      { name: "receivedBy", displayName: "Reçu par", type: "text", hideForm: true },
      { name: "receivedAt", displayName: "Date Réception", type: "datetime", hideForm: true },
      { name: "validated", displayName: "Validé", type: "boolean", hideForm: true },
    ],
    fieldsFilter: [
      { name: "boxType", displayName: "Type Boîte", type: "text" },
      { name: "boxId", displayName: "ID Boîte", type: "text" },
      { name: "sentBy", displayName: "Envoyé par", type: "text" },
      { name: "receivedBy", displayName: "Reçu par", type: "text" },
      { name: "validated", displayName: "Validé", type: "boolean" },
    ],
  },

  /*
    PartNumberWeight - Part Number Weight Configuration
  */
  partNumberWeight: {
    displayName: "Poids Part Number",
    operation: ["search", "create", "update", "delete"],
    firstOrderProperty: "partnumber",
    firstOrderDirection: "asc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "partnumber", displayName: "Part Number", type: "text", required: true },
      { name: "weightUnit", displayName: "Poids Unitaire (kg)", type: "number", required: true },
    ],
    fieldsFilter: [
      { name: "partnumber", displayName: "Part Number", type: "text" },
    ],
  },

  /*
    BoxTypeConfig - Box Type Configuration
  */
  boxTypeConfig: {
    displayName: "Configuration Type Boîte",
    operation: ["search", "create", "update", "delete"],
    firstOrderProperty: "boxType",
    firstOrderDirection: "asc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      {
        name: "boxType", displayName: "Type Boîte", type: "option", required: true, optionsList: [
          { label: "Grise", value: "gray" },
          { label: "Noire", value: "black" }
        ]
      },
      { name: "emptyBoxWeight", displayName: "Poids Boîte Vide (kg)", type: "number", required: true },
    ],
    fieldsFilter: [
      { name: "boxType", displayName: "Type Boîte", type: "text" },
    ],
  },

  /*
    CncPsSession - CNC PS Session History
  */
  cncPsSession: {
    displayName: "Historique Sessions CNC PS",
    operation: ["search"],
    firstOrderProperty: "createdAt",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "boxId", displayName: "ID Boîte", type: "text" },
      { name: "nSequenceImp", displayName: "Séquence", type: "text" },
      { name: "partNumberImp", displayName: "Part Number", type: "text" },
      { name: "code3Imp", displayName: "Code3", type: "text" },
      { name: "quantiteImp", displayName: "Quantité", type: "text" },
      { name: "operator", displayName: "Opérateur", type: "text" },
      { name: "productionStatus", displayName: "Statut Production", type: "text" },
      { name: "productionOperator", displayName: "Op. Production", type: "text" },
      { name: "createdAt", displayName: "Date création", type: "datetime", hideForm: true },
      { name: "completed", displayName: "Terminée", type: "boolean", hideForm: true },
      { name: "labelPrinted", displayName: "Étiquette imprimée", type: "boolean", hideForm: true },
    ],
    fieldsFilter: [
      { name: "boxId", displayName: "ID Boîte", type: "text" },
      { name: "nSequenceImp", displayName: "Séquence", type: "text" },
      { name: "partNumberImp", displayName: "Part Number", type: "text" },
      { name: "operator", displayName: "Opérateur", type: "text" },
      { name: "completed", displayName: "Terminée", type: "boolean" },
      { name: "productionStatus", displayName: "Statut Production", type: "text" },
    ],
  },

  /*
    CncPsLeatherConsumption - CNC PS Leather Consumption History
  */
  cncPsLeatherConsumption: {
    displayName: "Historique Consommations Cuir CNC PS",
    operation: ["search"],
    firstOrderProperty: "createdAt",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "leatherPartNumber", displayName: "PN Cuir", type: "text" },
      { name: "serial", displayName: "Serial", type: "text" },
      { name: "lot", displayName: "Lot", type: "text" },
      { name: "quantiteInitial", displayName: "Qté Initiale", type: "number" },
      { name: "quantiteConsumed", displayName: "Qté Consommée", type: "number" },
      { name: "quantiteRetour", displayName: "Qté Retour", type: "number" },
      { name: "createdAt", displayName: "Date", type: "datetime", hideForm: true },
    ],
    fieldsFilter: [
      { name: "leatherPartNumber", displayName: "PN Cuir", type: "text" },
      { name: "serial", displayName: "Serial", type: "text" },
      { name: "lot", displayName: "Lot", type: "text" },
    ],
  },

  /*
    ProgramCNC - CNC Program Configuration
  */
  programCNC: {
    displayName: "Programmes CNC",
    operation: ["Add", "Edit", "search", "Delete", "Export", "Import", "Supprimer"],
    firstOrderProperty: "id",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "partNumber", displayName: "Part Number", type: "text" },
      { name: "version", displayName: "Version", type: "text" },
      { name: "row", displayName: "Row", type: "text" },
      { name: "set", displayName: "Set", type: "text" },
      { name: "panelNumber", displayName: "Panel Number Final Shape", type: "text" },
      { name: "pattern", displayName: "Pattern", type: "text" },
      { name: "programNumber", displayName: "N° Programme", type: "text" },
      { name: "casette", displayName: "Cassette", type: "text" },
      { name: "coutureDecorativeCnc", displayName: "Fil Couture CNC", type: "text" },
      { name: "cavitePress", displayName: "Cavité Press", type: "text" },
      { name: "blindStitch", displayName: "Fil blind", type: "text" },
      { name: "profil", displayName: "Profil", type: "text" },
      {
        name: "type", displayName: "Type", type: "option", optionsList: [
          { value: "Insert", label: "Insert" },
          { value: "Upper", label: "Upper" },
        ]
      },
      { name: "code1", displayName: "Code1", type: "text" },

    ],
    // fieldsFilter: [
    //   { name: "partNumber", displayName: "Part Number", type: "text" },
    //   { name: "profil", displayName: "Profil", type: "text" },
    //   { name: "type", displayName: "Type", type: "text" },
    //   { name: "pattern", displayName: "Pattern", type: "text" },
    //   { name: "programNumber", displayName: "N° Programme", type: "text" },
    //   { name: "version", displayName: "Version", type: "text" },
    // ],
  },

  /*
    MachineCnc - CNC Machine Configuration
  */
  machineCnc: {
    displayName: "Machines CNC",
    operation: ["add", "edit", "search", "delete", "Export", "Import"],
    firstOrderProperty: "id",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "name", displayName: "Nom", type: "text" },
      { name: "type", displayName: "Type", type: "option", optionsList: [
        { label: "CNC", value: "CNC" },
        { label: "Press", value: "PRESS" },
        { label: "Blind", value: "BLIND" },
      ] },
    ],
    fieldsFilter: [
      { name: "name", displayName: "Nom", type: "text" },
      { name: "type", displayName: "Type", type: "text" },
    ],
  },

  /*
    ProgrammeDistribution - CNC Programme Distribution
  */
  programmeDistribution: {
    displayName: "Programme Distribution",
    operation: ["Add", "Edit", "search", "Delete", "Export", "Import"],
    firstOrderProperty: "id",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "machine", displayName: "Machine", type: "object", formDisplayProperty: "name", optionUrl: "/api/machineCnc/list" },
      { name: "programmeNumber", displayName: "N° Programme", type: "number" },
    ],
  },

  /*
    CncMachineReport - Imported CNC Machine Session Reports
  */
  cncMachineReport: {
    displayName: "Rapports Machine CNC",
    operation: ["search"],
    firstOrderProperty: "importedAt",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "machineName", displayName: "Machine", type: "text" },
      { name: "boxId", displayName: "ID Boîte", type: "text" },
      { name: "programNumber", displayName: "N° Programme", type: "text" },
      { name: "partNumber", displayName: "Part Number", type: "text" },
      { name: "operator", displayName: "Opérateur", type: "text" },
      { name: "productionStatus", displayName: "Statut", type: "text" },
      { name: "totalPieces", displayName: "Total Pièces", type: "number" },
      { name: "okPieces", displayName: "OK", type: "number" },
      { name: "defautPieces", displayName: "Défaut", type: "number" },
      { name: "scrapPieces", displayName: "Scrap", type: "number" },
      { name: "shiftNumber", displayName: "Shift", type: "text" },
      { name: "shiftDate", displayName: "Date Shift", type: "text" },
      { name: "importedAt", displayName: "Date Import", type: "datetime", hideForm: true },
      { name: "importedBy", displayName: "Importé Par", type: "text", hideForm: true },
    ],
    fieldsFilter: [
      { name: "machineName", displayName: "Machine", type: "text" },
      { name: "boxId", displayName: "ID Boîte", type: "text" },
      { name: "programNumber", displayName: "N° Programme", type: "text" },
      { name: "partNumber", displayName: "Part Number", type: "text" },
      { name: "operator", displayName: "Opérateur", type: "text" },
      { name: "shiftDate", displayName: "Date Shift", type: "text" },
    ],
  },

  /*
    CncMachineReportPiece - Pieces from imported CNC session reports
  */
  cncMachineReportPiece: {
    displayName: "Pièces Rapport CNC",
    operation: ["search"],
    firstOrderProperty: "id",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", hideForm: true },
      { name: "report", displayName: "Rapport", type: "object", formDisplayProperty: "id" },
      { name: "programNumber", displayName: "N° Programme", type: "text" },
      { name: "status", displayName: "Statut", type: "text" },
      { name: "qualityStatus", displayName: "Qualité", type: "text" },
      { name: "codeDefaut", displayName: "Code Défaut", type: "text" },
      { name: "codeScrap", displayName: "Code Scrap", type: "text" },
      { name: "qualityComment", displayName: "Commentaire", type: "text" },
      { name: "operatorUsername", displayName: "Opérateur", type: "text" },
      { name: "startDate", displayName: "Début", type: "datetime" },
      { name: "endDate", displayName: "Fin", type: "datetime" },
    ],
    fieldsFilter: [
      { name: "programNumber", displayName: "N° Programme", type: "text" },
      { name: "status", displayName: "Statut", type: "text" },
      { name: "qualityStatus", displayName: "Qualité", type: "text" },
      { name: "operatorUsername", displayName: "Opérateur", type: "text" },
    ],
  },

  /*
    PieceDetail - CAD Piece Details (imported from CSV)
  */
  pieceDetail: {
    displayName: "Détails Pièces (CAD)",
    operation: ["search", "delete"],
    firstOrderProperty: "pieceName",
    firstOrderDirection: "asc",
    fields: [
      { name: "pieceName", displayName: "Nom Pièce", type: "text" },
      { name: "descrip", displayName: "Description", type: "text" },
      { name: "category", displayName: "Catégorie", type: "text" },
      { name: "ruleTable", displayName: "Rule Table", type: "text" },
      { name: "area", displayName: "Surface (cm²)", type: "number" },
      { name: "totalArea", displayName: "Surface Totale (cm²)", type: "number" },
      { name: "perimeter", displayName: "Périmètre (cm)", type: "number" },
      { name: "pieceX", displayName: "Dimension X", type: "number" },
      { name: "pieceY", displayName: "Dimension Y", type: "number" },
      { name: "fabricCode", displayName: "Code Tissu", type: "text" },
      { name: "importedAt", displayName: "Date Import", type: "text", hideForm: true },
      { name: "importedBy", displayName: "Importé Par", type: "text", hideForm: true },
    ],
    fieldsFilter: [
      { name: "pieceName", displayName: "Nom Pièce", type: "text" },
      { name: "descrip", displayName: "Description", type: "text" },
      { name: "category", displayName: "Catégorie", type: "text" },
      { name: "fabricCode", displayName: "Code Tissu", type: "text" },
    ],
  },

  /*
    PartNumberInfo - Part Number reference information
  */
  partNumberInfo: {
    displayName: "Part Number Info",
    operation: ["search"],
    firstOrderProperty: "partNumber",
    firstOrderDirection: "asc",
    fields: [
      { name: "partNumber", displayName: "Part Number", type: "text", required: true },
      { name: "description", displayName: "Description", type: "text" },
      { name: "status", displayName: "Statut", type: "text" },
      { name: "prodLine", displayName: "Ligne de Production", type: "text" },
      { name: "ItemType", displayName: "Type Item", type: "text" },
      { name: "designGroup", displayName: "Groupe Design", type: "text" },
      { name: "itemGroup", displayName: "Groupe Item", type: "text" },
      { name: "covertype", displayName: "Cover Type", type: "text" },
      { name: "apd", displayName: "APD", type: "text" },
      { name: "packageQty", displayName: "Qté Emballage", type: "number" },
      { name: "weight", displayName: "Poids (kg)", type: "number" },
      { name: "totalPerimetre", displayName: "Périmètre Total (cm)", type: "number" },
      { name: "tempsDeCoupe", displayName: "Temps de Coupe (min)", type: "number" },
    ],
    fieldsFilter: [
      { name: "partNumber", displayName: "Part Number", type: "text" },
    ],
  },

  /*
    CapaciteInstallee - Installed capacity per group/shift
  */
  capaciteInstallee: {
    displayName: "Capacité Installée",
    operation: ["Add", "Edit", "Delete"],
    firstOrderProperty: "dateProduction",
    firstOrderDirection: "desc",
    fields: [
      { name: "id", displayName: "ID", type: "text", required: true, hideForm: true },
      { name: "dateProduction", displayName: "Date Production", type: "date", required: true },
      { name: "shiftNumber", displayName: "Shift", type: "option", required: true, optionsList: optionsShift },
      { name: "groupe", displayName: "Groupe", type: "option", required: true, optionsList: [
        { label: "Coupe", value: "Coupe" },
        { label: "Laser", value: "Laser" }
      ]},
      { name: "capaciteInstallee", displayName: "Capacité Installée", type: "number", required: true },
      { name: "tempsTotalParMachine", displayName: "Temps Total / Machine (min)", type: "number" },
      { name: "efficienceTarget", displayName: "Efficience Target (%)", type: "number" },
    ],
    fieldsFilter: [
      { name: "dateProduction", displayName: "Date Production", type: "date" },
      { name: "groupe", displayName: "Groupe", type: "text" },
    ],
  }
}