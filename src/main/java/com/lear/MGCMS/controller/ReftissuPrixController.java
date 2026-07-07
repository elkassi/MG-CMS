package com.lear.MGCMS.controller;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lear.MGCMS.domain.ReftissuPrix;
import com.lear.MGCMS.payload.WorkOrderElem;

@RestController
@RequestMapping("/api/reftissuPrix")
public class ReftissuPrixController {
	
	@GetMapping("/{reftissuArr}")
	public List<ReftissuPrix> findByReftissus(@PathVariable List<String> reftissuArr) throws NumberFormatException, IOException {
		List<String> reftissuDone = new ArrayList<String>();
		List<ReftissuPrix> arr = new ArrayList<ReftissuPrix>();
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(
					new InputStreamReader(new FileInputStream("C:\\pbsimport\\fichier3-6-15.prn"), "windows-1252"));
			List<String> liste = br.lines().collect(Collectors.toList());
			String ref = "";
			Double prix = 0.0;
			for (String ligne : liste) {
				if (ligne.length() > 93 && !ligne.startsWith("Item Number        Description              Sit")
						&& !ligne.startsWith("-----------------") && !ligne.startsWith("Product Line:")
						&& !ligne.contains("TANGIER-TRIM ") 
						&& !ligne.contains("3.6.15 Inventory Valuation as of Date")
						&& reftissuArr.contains(ligne.substring(0, 18).trim())
						) {
					ref = ligne.substring(0, 18).trim();
					prix = Double.parseDouble(ligne.substring(76, 93).trim().replace(",", ""));
					System.out.println("ref : " + ref + " prix : "+ prix);
					reftissuDone.add(ref);
					arr.add(new ReftissuPrix(ref,prix));
				}
				if(reftissuDone.size() == reftissuArr.size()) {
					System.out.println("Completed");

					break;
				}

			}

		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("Error : " + e.getMessage());
		} finally {
			System.out.println("DONE");
			try {
				br.close();
			} catch (Throwable e) {
			}
		}

		return arr;
	}

}
