package com.lear.MGCMS.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.domain.PartNumberInfo2;
import com.lear.MGCMS.domain.PartNumberBoom;
import com.lear.MGCMS.repositories.PartNumberInfo2Repository;
import com.lear.MGCMS.services.MapValidationErrorService;
import com.lear.MGCMS.services.PartNumberBoomService;

@RestController
@RequestMapping("/api/partNumberBoom")
public class PartNumberBoomController {

	@Autowired
	private PartNumberBoomService service;

	@Autowired
	private MapValidationErrorService mapValidationErrorService;

	@Autowired
	private PartNumberInfo2Repository partNumberInfo2Repository;



	@GetMapping("/all")
	public Page<PartNumberBoom> findAll(@RequestParam Map<String, String> filters,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
			@RequestParam(value = "size", defaultValue = "20", required = false) int size,
			@RequestParam(value = "sort", defaultValue = "createdAt,desc", required = false) String sortBy) {
		if (filters == null) {
			filters = new HashMap<>();
		}
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return service.findAll(filters, page, size, sortBy);
	}

	@GetMapping("/{id1}/{id2}")
	public ResponseEntity<?> findByCode(@PathVariable String id1, @PathVariable String id2) {
		PartNumberBoom obj = service.findByObjId(id1, id2);
		if (obj == null) {
			return new ResponseEntity<String>("Not found", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<PartNumberBoom>(obj, HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<?> save(@Valid @RequestBody PartNumberBoom obj, BindingResult result) {
		ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
		if (errorMap != null)
			return errorMap;
		PartNumberBoom newObj = service.save(obj);
		return new ResponseEntity<PartNumberBoom>(newObj, HttpStatus.CREATED);
	}

	@PostMapping("/update/{partNumbers}")
	public ResponseEntity<?> save(@PathVariable String partNumbers) {
		List<PartNumberBoom> arr = new ArrayList<PartNumberBoom>();
		List<String> partNumbersArr = new ArrayList<>(Arrays.asList(partNumbers.split(",")));

		String reportName = "13-8-8";
		String server = "10.49.0.46";// txtServer1.getText();
		int port = 21;
		String user = "mfg";// txtUserId1.getText();
		String pass = "leartsi01";// txtPassword1.getText();
		String qadLink = "/qad/home/batchtnr/" + reportName + ".prn";
		FTPClient ftpClient = new FTPClient();
		String line = "", lastLine = "";
		int i = -1;
		int percentage = 0;
		try {

			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();

			BufferedReader reader = new BufferedReader(new InputStreamReader(ftpClient.retrieveFileStream(qadLink)));

			String charging = null;
			PartNumberBoom obj = new PartNumberBoom();
			PartNumberInfo2 pnInfo2 = new PartNumberInfo2();
			String partnumber = null;
			while ((line = reader.readLine()) != null) {
				i++;
				
				while (percentage < (int) (1000 * ((float) i / 2001357))) {
					percentage++;
					System.out.println("Loading BOM UPDATE : " + i + "  " + percentage + "/1000 ");
				}
//				if(i < 73000) continue;
				if (line.contains("Quantity Per") || line.contains("------------")
						|| line.contains("13.8.8 Item-Site Structure Report") || line.contains("TANGIER-TRIM")
						|| line.contains("Site:") || line.contains("End of Report")
						|| line.contains("Report Submitted By") || line.contains("Parent Item")
						|| line.contains("Start:") || line.contains("Levels") || line.contains("New Page Each Parent")
						|| line.contains("Sort by Reference") || line.contains("Batch ID") || line.trim().isEmpty()) {
					continue;
				}

//				try {
				
				if (partNumbersArr.size() == 0 && line.startsWith("PARENT")) {
					
					if (partnumber != null && !line.substring(11, 29).trim().toUpperCase().equalsIgnoreCase(partnumber)
							&& obj.getPartNumber() != null && obj.getPartNumber().equalsIgnoreCase(partnumber)) {
						if (pnInfo2 != null) {
							obj.setProject(pnInfo2.getItemGroup());
							obj.setVersion(pnInfo2.getDesignGroup());
						}
						if(obj.getPartNumberMaterial() != null) {
							service.save(obj);
							String reftissu = obj.getPartNumberMaterial();
							arr.removeIf(elem -> elem.getPartNumberMaterial().equals(reftissu));
						}
					}
					if (arr.size() > 0) {
						service.deleteAll(arr);
					}
					break;
				}

				if (!line.startsWith("PARENT") && obj.getPartNumber() == null) {
					continue;
				}
				
				if (line.startsWith("PARENT")) {
					if (arr.size() > 0) {
						System.out.println("DELETE ARR : " +arr.size());
						service.deleteAll(arr);
					}

					if (partnumber != null && !line.substring(11, 29).trim().toUpperCase().equalsIgnoreCase(partnumber)
							&& obj.getPartNumber() != null && obj.getPartNumber().equalsIgnoreCase(partnumber)) {
						
						if (pnInfo2 != null) {
							obj.setProject(pnInfo2.getItemGroup());
							obj.setVersion(pnInfo2.getDesignGroup());
						}
						service.save(obj);
						System.out.println("last save : " + obj.getPartNumberMaterial() + " : " + obj.getPartNumber());

						String reftissu = obj.getPartNumberMaterial();
						arr.removeIf(elem -> elem.getPartNumberMaterial().equals(reftissu));

					}
					if (partNumbersArr.contains(line.substring(11, 29).trim().toUpperCase())) {
						if (partNumbersArr.size() == 0) {
							break;
						}
						System.out.println(line);
						obj = new PartNumberBoom();
						charging = null;
						partnumber = line.substring(11, 29).trim().toUpperCase();
						String pn = line.substring(11, 29).trim().toUpperCase();
						partNumbersArr.removeIf(elem -> elem.equalsIgnoreCase(pn));
						obj.setPartNumber(line.substring(11, 29).trim().toUpperCase());
						obj.setDescription(line.substring(43, Math.min(67, line.length())).trim().toUpperCase());
						arr = service.findByPartNumber(obj.getPartNumber());
						pnInfo2 = partNumberInfo2Repository.findByPartNumber(obj.getPartNumber());
					}

				}

				if (obj.getPartNumber() != null && !line.startsWith("PARENT")) {
					System.out.println(line);

					if (lastLine.startsWith("PARENT") && line.length() > 43 && !line.startsWith("1")) {
						obj.setDescription(
								obj.getDescription() + " " + line.substring(43, Math.min(67, line.length())).trim());
					} else if (charging == null && line.length() >= 83 && line.substring(81, 83).trim().toUpperCase().equalsIgnoreCase("EA")
							&& (line.substring(11, 29).trim().toUpperCase().startsWith("W")
									|| line.substring(11, 29).trim().startsWith("3"))) {
						obj.setItem(line.substring(11, 29).trim());
						if (line.substring(0, 4).trim().equals("1")) {
							charging = ".2";
						} else if (line.substring(0, 4).trim().equals(".2")) {
							charging = "..3";
						} else if (line.substring(0, 4).trim().equals("..3")) {
							charging = "...4";
						}
					} else if (charging != null && line.startsWith(charging)
							&& line.substring(81, 84).toUpperCase().trim().equalsIgnoreCase("MT") && line.length() > 84
							&& line.substring(84, Math.min(90, line.length())).trim().equals("5")
							) {//&& line.length() > 126
						System.out.println("charging : " + charging);
						if (obj.getPartNumberMaterial() != null && obj.getPartNumber() != null) {
								System.out.println(obj.toString());
							if (pnInfo2 != null) {
								obj.setProject(pnInfo2.getItemGroup());
								obj.setVersion(pnInfo2.getDesignGroup());
							}
							service.save(obj);
							System.out.println(obj.getPartNumberMaterial() + " : " + obj.getPartNumber());

							String reftissu = obj.getPartNumberMaterial();
							arr.removeIf(elem -> elem.getPartNumberMaterial().equals(reftissu));
						}
						obj.setQuantityPer(Double.parseDouble(line.substring(68, 80).trim()));
						obj.setPartNumberMaterial(line.substring(11, 29).trim().toUpperCase());
						obj.setPartNumberMaterialDescription(line.substring(43, 67).trim().toUpperCase());
					} else if (charging != null && lastLine.startsWith(charging) && line.length() > 43
							&& !line.startsWith("1") && !line.startsWith(".2") && !line.startsWith("..3")
							&& !line.startsWith("...4")) {
						obj.setPartNumberMaterialDescription(obj.getPartNumberMaterialDescription() + " "
								+ line.substring(43, Math.min(67, line.length())).trim().toUpperCase());
					}
				}

				lastLine = line;

			}
			System.out.println("DONE.");
		} catch (IOException e) {
			System.out.println("ERROR : " + e.getMessage());
		}

		return new ResponseEntity<List<PartNumberBoom>>(arr, HttpStatus.CREATED);
	}

	@Autowired
	private PartNumberBoomService partNumberBoomService;

	@GetMapping("/list")
	public List<PartNumberBoom> findList(@RequestParam(value = "project", required = false) String project,
			@RequestParam(value = "version", required = false) String version,
			@RequestParam(value = "partNumber", required = false) String partNumber,
			@RequestParam(value = "item", required = false) String item) {
		if(item == null && partNumber != null && partNumber.startsWith("L")) {
			item = "W" + partNumber;
		} else {
			List<PartNumberBoom> arr = partNumberBoomService.findList(project, version, partNumber);
			if(arr.size() > 0) {
				item = arr.get(0).getItem();
			}
		}
		return service.findList(project, version, partNumber, item);
	}

	@GetMapping("/pnMaterial/{pnMaterial}")
	public PartNumberBoom findByPartNumberMaterial(@PathVariable String pnMaterial) {
		return service.findFirstByPartNumberMaterial(pnMaterial);
	}

	@GetMapping("/items/{items}")
	public List<PartNumberBoom> findByItems(@PathVariable List<String> items) {
		return service.findByItems(items);
	}

	/**
	 * Get partNumberMaterials for a list of partNumber-item pairs
	 * Body should be a list of objects with partNumber and item fields
	 */
	@PostMapping("/byPartNumbersAndItems")
	public List<String> findByPartNumbersAndItems(@RequestBody List<Map<String, String>> partNumberItemPairs) {
		return service.findPartNumberMaterialsByPartNumbersAndItems(partNumberItemPairs);
	}

}
