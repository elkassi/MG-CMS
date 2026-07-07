package com.lear.MGCMS.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lear.MGCMS.domain.CuttingPlan.data.CuttingPlanMaterialPlacementData;
import com.lear.MGCMS.domain.Placement;
import com.lear.MGCMS.domain.PlacementFolder;
import com.lear.MGCMS.payload.EmpiecementRapport;
import com.lear.MGCMS.repositories.PlacementFolderRepository;
import com.lear.MGCMS.services.CuttingPlan.data.CuttingPlanMaterialPlacementDataService;
import com.lear.MGCMS.services.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.PlacementDetail;
import com.lear.MGCMS.payload.EmpStat;
import com.lear.MGCMS.payload.Stat2Label1Value;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.PlacementDetailService;
import com.lear.MGCMS.services.UserService;

@RestController
@RequestMapping("/api/placementDetail")
public class PlacementDetailController {
	
	@Autowired
	private PlacementDetailService service;
	
	@Autowired
	private MapValidationErrorService mapValidationErrorService;
	
	@Autowired
	private UserService userService;
	
	@GetMapping("/stats")
	public List<EmpStat> findSats() {
		return service.findSats();
	}
	
	@GetMapping("/all")
	public Page<PlacementDetail> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy
			) {
		if (filters == null) {
            filters = new HashMap<>();
        }
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return service.findAll(filters, page,size,sortBy);
	}
	@Autowired
	private PlacementFolderRepository placementFolderRepository;
	@Autowired
	private CuttingPlanMaterialPlacementDataService cuttingPlanMaterialPlacementDataService;
	@GetMapping("/empV2")
	public List<EmpiecementRapport> empV2(
			@RequestParam(value = "pattern", required = true) String pattern,
			@RequestParam(value = "placementFormat", required = true) String placementFormat
	) {
		List<PlacementFolder> folders = placementFolderRepository.findAll();
		List<EmpiecementRapport> arr = new ArrayList<>();
		HashMap<String, String> drillCMSMap = new HashMap<>();
		for (PlacementFolder pf : folders) {
			System.out.println("Folder : " + pf.getFolderLink());
			File folder = new File(pf.getFolderLink());
			File[] listOfFiles = folder.listFiles();
			for (int j = 0; j < listOfFiles.length; j++) {
				if(!listOfFiles[j].getName().startsWith(placementFormat)
						&& !listOfFiles[j].getName().startsWith("__"+placementFormat)) {
					continue;
				}
//				System.out.println("File " + listOfFiles[j].getName() + " : " + j + " / " + listOfFiles.length);

				Placement placement = new Placement();
				if (listOfFiles[j].isFile()) {
					long lastModifiedTimestamp = listOfFiles[j].lastModified();
					// convert lastModifiedTimestamp ot LocalDateTime
					LocalDateTime lastModified = Instant.ofEpochMilli(lastModifiedTimestamp).atZone(ZoneId.systemDefault())
							.toLocalDateTime();
					List<String> tempList = new ArrayList<>();
					BufferedReader br = null;
					try {
						br = new BufferedReader(
								new InputStreamReader(new FileInputStream(listOfFiles[j]), "windows-1252"));
						if (br != null) {

							placement.setPlacement(listOfFiles[j].getName());
                            placement.setFolder(pf.getFolderLink());
                            placement.setLastModified(lastModifiedTimestamp);


							String[] liste = br.lines().collect(Collectors.toList())
									.toArray(new String[br.lines().collect(Collectors.toList()).size()]);
							String digit = null;
							Integer empInd = null;
							String partNumberMaterial = null;
							String description = null, categoriePiece = null, taille = null, idPaquet = null,
									nomMedele = null;
							int nbrPieces = 0;
							for (int i = 1; i < liste.length; i++) {
								if (partNumberMaterial == null && (
										liste[i].startsWith("M,NUMERO")
										|| liste[i].startsWith("M,ORDER NUM")
								)) {
									partNumberMaterial = liste[i].split(",")[2];
									placement.setPartNumberMaterial(partNumberMaterial);
								}
								if (liste[i].startsWith("M,Efficience plct")) {
									placement.setEfficience(Double.parseDouble(liste[i].split(",")[2]));
								}

								if (liste[i].startsWith("L,")) {
									empInd = Integer.parseInt(liste[i].split(",")[1]);
									digit = null;
								} else if (liste[i].startsWith("D,1,")) {
									digit = liste[i].split(",")[2];
								} else if (liste[i].startsWith("D,2,")) {
									description = liste[i].split(",")[2];
								} else if (liste[i].startsWith("D,3,")) {
									categoriePiece = liste[i].split(",")[2];
								} else if (liste[i].startsWith("D,4,")) {
									taille = liste[i].split(",")[2];
								} else if (liste[i].startsWith("D,5,")) {
									idPaquet = liste[i].split(",")[2];
								} else if (liste[i].startsWith("D,6,")) {
									nomMedele = liste[i].split(",")[2];
								} else if (liste[i].startsWith("D,7,")) {
									if (digit != null) {
										EmpiecementRapport placementDetail = new EmpiecementRapport();
										placementDetail.setFolder(placement.getFolder());
										placementDetail.setPlacement(placement.getPlacement());
										placementDetail.setReftissu(partNumberMaterial);
										placementDetail.setPartnumber(nomMedele);
										placementDetail.setDate(lastModified);
//                                        placementDetail.setGaucheDroite(liste[i].split(",")[2]);
										digit.replace("-LSR", "");
										nbrPieces++;
										if(digit.trim().toUpperCase().equalsIgnoreCase(pattern.toUpperCase().trim())) {
											if(!tempList.contains(placementDetail.getPartnumber())) {
												arr.add(placementDetail);
												tempList.add(placementDetail.getPartnumber());
											}
										}
										digit = null;
									}
								}
							}
						}
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}
		}
		return arr;
	}
	
	@GetMapping("/emp")
	public List<Stat2Label1Value> returnEmpStat(
			@RequestParam(value = "pattern", required = true) String pattern
	) {
		return service.returnEmpStat(pattern);
	}

}
