package com.lear.MGCMS.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lear.MGCMS.domain.PieceDetail;
import com.lear.MGCMS.services.PieceDetailService;
import com.lear.MGCMS.services.PartNumberWeightCalculationService;

@RestController
@RequestMapping("/api/pieceDetail")
public class PieceDetailController {

	@Autowired
	private PieceDetailService service;

	@Autowired
	private PartNumberWeightCalculationService weightCalculationService;

	// Import CSV file
	@PostMapping("/import")
	public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file, Authentication authentication) {
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Le fichier est vide"));
		}
		String username = authentication.getName();
		Map<String, Object> result = service.importCsv(file, username);
		return ResponseEntity.ok(result);
	}

	// Get all pieces (paginated)
	@GetMapping("/all")
	public Page<PieceDetail> findAll(
			@RequestParam Map<String, String> filters,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(value = "sort", defaultValue = "pieceName,desc", required = false) String sort
	) {
		if (filters == null) {
			filters = new HashMap<>();
		}
		filters.remove("page");
		filters.remove("size");
		filters.remove("sort");
		return service.findAll(filters, page,size, sort);
	}

	// Get all pieces (non-paginated)
	@GetMapping("/list")
	public List<PieceDetail> list() {
		return service.findAll();
	}

	// Get single piece detail
	@GetMapping("/{pieceName}")
	public ResponseEntity<PieceDetail> findById(@PathVariable String pieceName) {
		Optional<PieceDetail> pd = service.findById(pieceName);
		return pd.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// Search by description
	@GetMapping("/search")
	public List<PieceDetail> searchByDescrip(@RequestParam String descrip) {
		return service.findByDescripContaining(descrip);
	}

	// Delete piece detail
	@DeleteMapping("/{pieceName}")
	public ResponseEntity<?> delete(@PathVariable String pieceName) {
		Optional<PieceDetail> pd = service.findById(pieceName);
		if (pd.isPresent()) {
			service.deleteById(pieceName);
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.notFound().build();
	}

	// Calculate weight for given partNumberCovers
	@PostMapping("/calculateWeight")
	public ResponseEntity<?> calculateWeight(@RequestBody Map<String, List<String>> body) {
		List<String> partNumberCovers = body.get("partNumberCovers");
		if (partNumberCovers == null || partNumberCovers.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "partNumberCovers est requis"));
		}
		List<Map<String, Object>> results = weightCalculationService.calculateWeights(partNumberCovers);
		return ResponseEntity.ok(results);
	}
}
