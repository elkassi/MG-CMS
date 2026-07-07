package com.lear.MGCMS.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventoryDetail")
public class InventoryDetailController {

	@GetMapping
	public List<InventoryDetail> findAll() throws IOException {
		List<InventoryDetail> arr = new ArrayList<InventoryDetail>();

		String server = "10.49.0.154";// txtServer1.getText();
		int port = 21;
		String user = "rguenda";// txtUserId1.getText();
		String pass = "Tanger.2022";// txtPassword1.getText();
		String qadLink = "/qad/home/batchtnr/3-6-6.prn";
		FTPClient ftpClient = new FTPClient();

		try {
			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();

			BufferedReader reader = new BufferedReader(new InputStreamReader(ftpClient.retrieveFileStream(qadLink)));

			String line;
			String site = "", location = "", item = "";
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty() || line.length() < 18 || line.contains("3.6.6 Inventory Detail by Location")
						|| line.contains("------------") || line.contains("Lot/Serial") || line.contains("Qty on Hand") || line.contains("TANGIER-TRIM")
						|| line.contains("End of Report") || line.contains("Report Submitted By")
						|| line.contains("To:") || line.contains("Output:") || line.contains("Batch ID:")) {
					continue;
				}

				if (line.length() <= 72) {
					if (line.substring(18, line.length()).trim().isEmpty()) {
						continue;
					} else {
						site = line.substring(0, 8);
						location = line.substring(9, 17);
						item = line.substring(18, line.length());
					}

				} else {
					if (!line.substring(18, 36).trim().isEmpty()) {
						site = line.substring(0, 8);
						location = line.substring(9, 17);
						item = line.substring(18, 36);
					}
					try {
						Double qte = 0.0;
						if (!line.substring(59, 72).trim().isEmpty()) {
							try {
								qte = Double.parseDouble(line.substring(59, 72));
							} catch (Exception e) {
								System.out.println(line);
								System.out.println("Exception : " + e.getMessage());
							}
						}

						InventoryDetail obj = new InventoryDetail(site.trim(), location.trim(), item.trim(), line.substring(37, 55).trim(),
								line.substring(56, 58).trim(), qte, line.substring(73, 81).trim(), line.substring(82, 90).trim(),
								line.substring(91, 98).trim(), line.substring(99, 104).trim(), line.substring(105, 113).trim(),
								line.substring(114, 119).trim(), line.substring(120, 123).trim(), line.substring(124, line.length()).trim());
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");

//			        obj.setSite(line.substring(0,8));
//			        obj.setLocation(line.substring(9,17));
//			        obj.setItem(line.substring(18,36));
//			        obj.setSerial(line.substring(37,55));
//			        obj.setUnity(line.substring(56,58));
//			        obj.setQtyOnHand(line.substring(59,72));
//			        obj.setCreated(line.substring(73,81));
//			        obj.setExpired(line.substring(82,90));
//			        obj.setAssay(line.substring(91,98));
//			        obj.setGrade(line.substring(99,104));
//			        obj.setStatus(line.substring(105, 113));
//			        obj.setAvailable(line.substring(114,119));
//			        obj.setNet(line.substring(120,123));
//			        obj.setOvrIs(line.substring(124,line.length()));
						arr.add(obj);
					} catch (Exception e) {
						System.out.println(line);
						System.out.println("Exception : " + e.getMessage());
					}
				}

			}

			reader.close();
			ftpClient.completePendingCommand();
		} finally {
			ftpClient.disconnect();
		}

		return arr;
	}

}

class InventoryDetail {
	private String site;
	private String location;
	private String item;
	private String serial;
	private String unity;
	private Double qtyOnHand;
	private String created;
	private String expired;
	private String assay;
	private String grade;
	private String status;
	private String available;
	private String net;
	private String ovrIs;

	public InventoryDetail() {
		super();
	}

	public InventoryDetail(String site, String location, String item, String serial, String unity, Double qtyOnHand,
			String created, String expired, String assay, String grade, String status, String available, String net,
			String ovrIs) {
		super();
		this.site = site;
		this.location = location;
		this.item = item;
		this.serial = serial;
		this.unity = unity;
		this.qtyOnHand = qtyOnHand;
		this.created = created;
		this.expired = expired;
		this.assay = assay;
		this.grade = grade;
		this.status = status;
		this.available = available;
		this.net = net;
		this.ovrIs = ovrIs;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public String getExpired() {
		return expired;
	}

	public void setExpired(String expired) {
		this.expired = expired;
	}

	public String getAssay() {
		return assay;
	}

	public void setAssay(String assay) {
		this.assay = assay;
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getOvrIs() {
		return ovrIs;
	}

	public void setOvrIs(String ovrIs) {
		this.ovrIs = ovrIs;
	}

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public String getUnity() {
		return unity;
	}

	public void setUnity(String unity) {
		this.unity = unity;
	}

	public Double getQtyOnHand() {
		return qtyOnHand;
	}

	public void setQtyOnHand(Double qtyOnHand) {
		this.qtyOnHand = qtyOnHand;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAvailable() {
		return available;
	}

	public void setAvailable(String available) {
		this.available = available;
	}

	public String getNet() {
		return net;
	}

	public void setNet(String net) {
		this.net = net;
	}

}
