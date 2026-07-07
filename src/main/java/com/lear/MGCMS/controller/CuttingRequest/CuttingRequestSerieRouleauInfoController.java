package com.lear.MGCMS.controller.CuttingRequest;

import com.lear.MGCMS.domain.CuttingRequest.CuttingRequestSerieRouleauInfo;
import com.lear.MGCMS.payload.ProdTicket;
import com.lear.MGCMS.payload.RouleauRapport;
import com.lear.MGCMS.security.Constants;
import com.lear.MGCMS.services.CuttingRequest.CuttingRequestSerieRouleauInfoService;
import com.lear.MGCMS.services.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cuttingRequestSerieRouleauInfo")
public class CuttingRequestSerieRouleauInfoController {
	
	@Autowired
	private CuttingRequestSerieRouleauInfoService service;

	@Autowired
	private QueryService queryService;
	
	@GetMapping("/rouleauRapport")		
	public List<RouleauRapport> findRest(
			@RequestParam(value = "reftissu", required = false) String reftissu
	) {
		return service.findRest(reftissu);
	}

	
	@GetMapping("/idRouleau")
	public ResponseEntity<?> findByIdRouleau(
			@RequestParam(value = "idRouleau", required = false) String idRouleau,
			@RequestParam(value = "reftissu", required = false) String reftissu
	) {
		try {
			RouleauRapport rr = new RouleauRapport();

			CuttingRequestSerieRouleauInfo crsri = new CuttingRequestSerieRouleauInfo();
			List<CuttingRequestSerieRouleauInfo> crsriArr = service.findByIdRouleau(idRouleau);
			if (crsriArr.size() != 0) {
				crsri = crsriArr.get(0);
				if (reftissu != null && !crsri.getConfirmReftissu().equalsIgnoreCase(reftissu)) {
					return new ResponseEntity<String>("Le rouleau " + idRouleau + " n'est pas associé à la référence tissu " + reftissu, org.springframework.http.HttpStatus.BAD_REQUEST);
				}
				if (crsri.getConfirmRetour() == false) {
					if (crsri.getDefaut() > 0 && crsri.getDefautCode() != null) {
						return new ResponseEntity<String>("Le rouleau " + idRouleau + " est défectueux (en attente de la validation de qualité réception)", org.springframework.http.HttpStatus.BAD_REQUEST);
					}
					return new ResponseEntity<String>("Le rouleau " + idRouleau + " est consommé", org.springframework.http.HttpStatus.BAD_REQUEST);
				}
			}

			ProdTicket pt = queryService.findByLabelId("S" + idRouleau);
			if (pt != null && pt.getQuantity() <= 0.1) {
				return new ResponseEntity<String>("le rouleau " + idRouleau + " est consommé dans PLS", org.springframework.http.HttpStatus.BAD_REQUEST);
			}

			if (pt != null && pt.getQuantity() != null && (crsri == null || (crsri.getRetour() != null && pt.getQuantity() < crsri.getRetour()))) {
				//if loh hr start with h or H , then remove it
				if (pt.getLotNr().startsWith("H") || pt.getLotNr().startsWith("h")) {
					pt.setLotNr(pt.getLotNr().substring(1));
				}
				rr.setLotFrs(pt.getLotNr());
				rr.setIdRouleau(pt.getLabelId());
				rr.setRetour(pt.getQuantity());
				rr.setSerie(pt.getPlsId());
				rr.setCreatedAt(pt.getCreatedAt());
				rr.setTableMatelassage(pt.getTableName());
				return ResponseEntity.ok(rr);
			} else if (crsri != null) {
				rr.setLotFrs(crsri.getLotFrs());
				rr.setIdRouleau(crsri.getIdRouleau());
				rr.setRetour(crsri.getRetour());
				if (crsri.getCuttingRequestSerie() != null) {
					rr.setSerie(crsri.getCuttingRequestSerie().getSerie());
					rr.setTableMatelassage(crsri.getCuttingRequestSerie().getTableMatelassage());
				}
				rr.setCreatedAt(crsri.getCreatedAt());
				rr.setLaize(crsri.getLaize());
				return ResponseEntity.ok(rr);
			} else {
				return ResponseEntity.ok(rr);
			}
		} catch (Exception e) {
			Constants.writeLogs("Error : " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				Constants.writeLogs(element.toString());
			}
		}
		return ResponseEntity.ok("good");

	}



}
