package com.lear.MGCMS.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lear.MGCMS.domain.PieceDetail;
import com.lear.MGCMS.repositories.PieceDetailRepository;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

@Service
public class PieceDetailService {

	@Autowired
	private PieceDetailRepository pieceDetailRepository;

	public List<PieceDetail> findAll() {
		return pieceDetailRepository.findAll();
	}

	public Page<PieceDetail> findAll(Map<String, String> filters, int page, int size, String sort) {
		String[] sortArr = sort.split(",");
		String evalSort = sortArr[0];
		String sortDirection = sortArr[1];
		Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
		Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

		Specification<PieceDetail> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Add filters based on the key-value pairs in the 'filters' map
			for (Map.Entry<String, String> entry : filters.entrySet()) {
				System.out.println(entry.getKey() + " : "+ entry.getValue());
				String[] strArr = entry.getKey().split("\\.");

				if(strArr.length >= 2) {
					Path<String> path = root.get(strArr[1]);
					for(int i = 2; i < strArr.length; i++) {
						path = path.get(strArr[i]);
					}

					// Handle different data types
					if (path.getJavaType().equals(String.class)) {
						System.out.println("String");
						if (entry.getKey().startsWith("startWith.")) {
							predicates.add(builder.like(path.as(String.class), entry.getValue() + "%"));
						} else if (entry.getKey().startsWith("endWith.")) {
							predicates.add(builder.like(path.as(String.class), "%" + entry.getValue()));
						} else if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(String.class), entry.getValue()));
						} else if (entry.getKey().startsWith("notEqual.")) {
							predicates.add(builder.notEqual(path.as(String.class), entry.getValue()));
						} else if (entry.getKey().startsWith("contains.")) {
							predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
						}
					} else if (path.getJavaType().equals(Boolean.class)) {
						String valueEntry = entry.getValue();
						if(valueEntry.equalsIgnoreCase("1")) {
							valueEntry = "TRUE";
						}
						if(valueEntry.equalsIgnoreCase("0")) {
							valueEntry = "FALSE";
						}
						if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(Boolean.class), Boolean.parseBoolean(valueEntry)));
						} else if (entry.getKey().startsWith("notEqual.")) {
							predicates.add(builder.notEqual(path.as(Boolean.class), Boolean.parseBoolean(valueEntry)));
						}
					} else if (path.getJavaType().equals(LocalDate.class)) {
						System.out.println("LocalDate");
						if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
						} else if (entry.getKey().startsWith("greaterThan.")) {
							predicates.add(builder.greaterThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
						} else if (entry.getKey().startsWith("lessThan.")) {
							predicates.add(builder.lessThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
						}
//                        else if (entry.getKey().startsWith("startWith.")) {
//                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS".substring(0, entry.getValue().length())));
//                            Integer lg = entry.getValue().length() ;
//                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
//                            switch(lg) {
//                            	case 1 :
//                            		endDateTime = startDateTime.plusYears(1000).minusNanos(1);break;
//                            	case 2 :
//                            		endDateTime = startDateTime.plusYears(100).minusNanos(1);break;
//                            	case 3:
//                            		endDateTime = startDateTime.plusYears(10).minusNanos(1);break;
//
//                            }
//                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
//                        }
					} else if (path.getJavaType().equals(LocalDateTime.class)) {
						System.out.println("LocalDateTime");
						if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
						} else if (entry.getKey().startsWith("greaterThan.")) {
							predicates.add(builder.greaterThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
						} else if (entry.getKey().startsWith("lessThan.")) {
							predicates.add(builder.lessThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
						} else if (entry.getKey().startsWith("startWith.")) {
							LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue());
							LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
							predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
						}
					} else if (path.getJavaType().equals(Integer.class)) {
						// Handle Integer conditions
						if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(Integer.class), Integer.parseInt(entry.getValue())));
						} else if (entry.getKey().startsWith("greaterThan.")) {
							predicates.add(builder.greaterThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
						} else if (entry.getKey().startsWith("lessThan.")) {
							predicates.add(builder.lessThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
						}
					} else if (path.getJavaType().equals(Double.class)) {
						// Handle Double conditions
						if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(Double.class), Double.parseDouble(entry.getValue())));
						} else if (entry.getKey().startsWith("greaterThan.")) {
							predicates.add(builder.greaterThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
						} else if (entry.getKey().startsWith("lessThan.")) {
							predicates.add(builder.lessThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
						}
					} else if (path.getJavaType().equals(Long.class)) {
						// Handle Long conditions
						if (entry.getKey().startsWith("equal.")) {
							predicates.add(builder.equal(path.as(Long.class), Long.parseLong(entry.getValue())));
						} else if (entry.getKey().startsWith("greaterThan.")) {
							predicates.add(builder.greaterThan(path.as(Long.class), Long.parseLong(entry.getValue())));
						} else if (entry.getKey().startsWith("lessThan.")) {
							predicates.add(builder.lessThan(path.as(Long.class), Long.parseLong(entry.getValue())));
						}
					}

//                    if (entry.getKey().startsWith("equal.")) {
//                        predicates.add(builder.equal(path, entry.getValue()));
//                    } else if (entry.getKey().startsWith("notEqual.")) {
//                        predicates.add(builder.notEqual(path, entry.getValue()));
//                    } else if (entry.getKey().startsWith("contains.")) {
//                        predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
//                    }

					if(entry.getKey().startsWith("isNull.")) {
						predicates.add(builder.isNull(path));
					} else if(entry.getKey().startsWith("isNotNull.")) {
						predicates.add(builder.isNotNull(path));
					}

//                	if(entry.getKey().startsWith("startWith.")) {
//                        predicates.add(builder.like(path, entry.getValue()+"%"));
//                	} else if(entry.getKey().startsWith("endWith.")) {
//                        predicates.add(builder.like(path, "%"+entry.getValue()));
//                	} else if(entry.getKey().startsWith("equal.")) {
//                        predicates.add(builder.equal(path, entry.getValue()));
//                	} else if(entry.getKey().startsWith("notEqual.")) {
//                        predicates.add(builder.notEqual(path, entry.getValue()));
//                	} else if(entry.getKey().startsWith("contains.")) {
//                        predicates.add(builder.like(path, "%"+entry.getValue()+"%"));
//                	} else if(entry.getKey().startsWith("greaterThan.")) {
//                        predicates.add(builder.greaterThan(path, entry.getValue()));
//                	} else if(entry.getKey().startsWith("lessThan.")) {
//                        predicates.add(builder.lessThan(path, entry.getValue()));
//                	} else if(entry.getKey().startsWith("isNull.")) {
//                        predicates.add(builder.isNull(path));
//                	} else if(entry.getKey().startsWith("isNotNull.")) {
//                        predicates.add(builder.isNotNull(path));
//                	}
				}
			}

			return builder.and(predicates.toArray(new Predicate[0]));
		};

		return pieceDetailRepository.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
	}

	private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
		if (sortDirection.equalsIgnoreCase("desc")){
			return Sort.Direction.DESC;
		} else {
			return Sort.Direction.ASC;
		}
	}

	public Optional<PieceDetail> findById(String pieceName) {
		return pieceDetailRepository.findById(pieceName);
	}

	public List<PieceDetail> findByDescripContaining(String descrip) {
		return pieceDetailRepository.findByDescripContaining(descrip);
	}

	public PieceDetail save(PieceDetail pieceDetail) {
		return pieceDetailRepository.save(pieceDetail);
	}

	public void deleteById(String pieceName) {
		pieceDetailRepository.deleteById(pieceName);
	}

	/**
	 * Import CSV file containing piece details from CAD software export.
	 * Returns a map with "imported" count and "errors" list.
	 */
	public Map<String, Object> importCsv(MultipartFile file, String importedBy) {
		List<String> errors = new ArrayList<>();
		int imported = 0;
		int lineNumber = 0;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String headerLine = reader.readLine();
			lineNumber++;
			if (headerLine == null) {
				errors.add("Le fichier CSV est vide");
				return Map.of("imported", 0, "errors", errors);
			}

			// Parse header to determine column indices
			String[] headers = headerLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
			Map<String, Integer> headerMap = new HashMap<>();
			for (int i = 0; i < headers.length; i++) {
				headerMap.put(headers[i].trim().replace("\"", ""), i);
			}

			String line;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				try {
					String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

					PieceDetail pd = new PieceDetail();
					pd.setPieceName(getStringValue(values, headerMap, "Piece Name"));
					if (pd.getPieceName() == null || pd.getPieceName().isEmpty()) {
						errors.add("Erreur ligne " + lineNumber + ": Piece Name est vide");
						continue;
					}

					pd.setDescrip(getStringValue(values, headerMap, "DESCRIP"));
					pd.setCategory(getStringValue(values, headerMap, "CATEGORY"));
					pd.setComment(getStringValue(values, headerMap, "COMMENT"));
					pd.setRuleTable(getStringValue(values, headerMap, "RULE TABLE"));
					pd.setByteSize(getIntValue(values, headerMap, "BYTE SIZE"));
					pd.setArea(getDoubleValue(values, headerMap, "Area"));
					pd.setTotalArea(getDoubleValue(values, headerMap, "Total Area"));
					pd.setPerimeter(getDoubleValue(values, headerMap, "Perimeter"));
					pd.setBaseSize(getDoubleValue(values, headerMap, "Base Size"));
					pd.setSmallestSize(getDoubleValue(values, headerMap, "Smallest Size"));
					pd.setNumInt(getIntValue(values, headerMap, "NUM INT"));
					pd.setNumNch(getIntValue(values, headerMap, "NUM NCH"));
					pd.setNumGp(getIntValue(values, headerMap, "NUM GP"));
					pd.setNumCrn(getIntValue(values, headerMap, "NUM CRN"));
					pd.setPieceX(getDoubleValue(values, headerMap, "PIECE X"));
					pd.setPieceY(getDoubleValue(values, headerMap, "PIECE Y"));
					pd.setShrinkStretchX(getStringValue(values, headerMap, "Shrink/Stretch X"));
					pd.setShrinkStretchY(getStringValue(values, headerMap, "Shrink/Stretch Y"));
					pd.setFabricCode(getStringValue(values, headerMap, "Fabric Code"));
					pd.setDate(getStringValue(values, headerMap, "DATE"));
					pd.setUserLastMod(getStringValue(values, headerMap, "User Last Mod"));
					pd.setCreatedTime(getStringValue(values, headerMap, "Created Time"));
					pd.setUserCreated(getStringValue(values, headerMap, "User Created"));
					pd.setPrevModTime(getStringValue(values, headerMap, "Prev Mod Time"));
					pd.setUserPrevMod(getStringValue(values, headerMap, "User Prev Mod"));
					pd.setImportedAt(LocalDateTime.now());
					pd.setImportedBy(importedBy);

					pieceDetailRepository.save(pd);
					imported++;
				} catch (Exception e) {
					errors.add("Erreur ligne " + lineNumber + ": " + e.getMessage());
				}
			}
		} catch (Exception e) {
			errors.add("Erreur lors de la lecture du fichier: " + e.getMessage());
		}

		Map<String, Object> result = new HashMap<>();
		result.put("imported", imported);
		result.put("errors", errors);
		return result;
	}

	private String getStringValue(String[] values, Map<String, Integer> headerMap, String column) {
		Integer index = headerMap.get(column);
		if (index == null || index >= values.length) return null;
		String val = values[index].trim().replace("\"", "");
		return val.isEmpty() ? null : val;
	}

	private Double getDoubleValue(String[] values, Map<String, Integer> headerMap, String column) {
		String val = getStringValue(values, headerMap, column);
		if (val == null) return null;
		try {
			return Double.parseDouble(val);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Integer getIntValue(String[] values, Map<String, Integer> headerMap, String column) {
		String val = getStringValue(values, headerMap, column);
		if (val == null) return null;
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
