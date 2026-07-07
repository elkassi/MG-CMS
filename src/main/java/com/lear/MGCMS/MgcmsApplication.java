package com.lear.MGCMS;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.lear.MGCMS.domain.Role;
import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.security.Constants;
import com.lear.MGCMS.services.RoleService;
import com.lear.MGCMS.services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.lear.MGCMS.storage.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
@EnableScheduling
public class MgcmsApplication {

    static String port = "8085";//6001 8085

    private static boolean isPortInUse(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private static void openBrowser(String url) {
        Runtime rt = Runtime.getRuntime();
        try {
            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        try {
            if (!isPortInUse(Integer.parseInt(port))) {
                SpringApplication.run(MgcmsApplication.class, args);
                openBrowser("http://localhost:" + port);
                Constants.writeLogs("Start: Server started on port " + port);
            } else {
                openBrowser("http://localhost:" + port);
                Constants.writeLogs("Start: Port " + port + " is already in use");
            }
        } catch (Exception e) {
            Constants.writeLogs(e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                Constants.writeLogs(element.toString());
            }
        }
    }


    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner createNewUser(UserService userService, RoleService roleService, BCryptPasswordEncoder bCryptPasswordEncoder) {
        return args -> {

            // List of all roles to verify
            String[][] rolesList = {
                    {"ROLE_ADMIN", "Administrateur"},
                    {"ROLE_CAD", "CAD"},
                    {"ROLE_ENGINEERING", "Ingénierie"},
                    {"ROLE_IMPORTER", "Importateur"},
                    {"ROLE_MATELASSEUR", "Matelasseur"},
                    {"ROLE_COUPEUR", "Coupeur"},
                    {"ROLE_QUALITE", "Qualité"},
                    {"ROLE_PLS_READER", "PLS reader"},
                    {"ROLE_PLS_ADMIN", "PLS admin"},
                    {"ROLE_SPLICE_READER", "Splice reader"},
                    {"ROLE_CUTTING_CUIR", "Cutting Cuir"},
                    {"ROLE_PROCESS", "Process"},
                    {"ROLE_GT_READER", "GT Reader"},
                    {"ROLE_CHEF_EQUIPE", "Chef d'équipe"},
                    {"ROLE_USER", "Utilisateur"},
                    {"ROLE_MAINTENANCE", "Maintenance"},
                    {"ROLE_CHEF_DE_ZONE", "Chef de zone"},
                    {"ROLE_VARIANCE", "Variance"},
                    {"ROLE_INDICATEUR", "Indicateur"},
                    {"ROLE_QN", "QN"},
                    {"ROLE_VALID_QN_FOURNISSEUR", "QN Validation fournisseur"},
                    {"ROLE_VALID_QN_LOGISTIQUE", "QN Validation logistique"},
                    {"ROLE_QN_SUPERVISOR", "QN Superviseur"},
                    {"ROLE_DB_PROD_READER", "DB Production Reader"},
                    {"ROLE_CNC_PS", "CNC PS"}
            };

            // Ensure all roles exist in the database
            for (String[] roleData : rolesList) {
                String roleName = roleData[0];
                String roleDescription = roleData[1];
                if (roleService.findByName(roleName) == null) {
                    roleService.save(new Role(roleName, roleDescription));
                }
            }

            // Check if an admin user exists
            boolean adminFound = false;
            for (User user : userService.findAll()) {
                for (Role role : user.getRoles()) {
                    if (role.getName().equalsIgnoreCase("ROLE_ADMIN")) {
                        adminFound = true;
                        break;
                    }
                }
                if (adminFound) break;
            }

            // Create an admin user if not found
            if (!adminFound) {
                User user = new User();
                user.setUsername("admin");
                user.setFirstName("admin");
                user.setLastName("app");
                user.setMatricule("0001");
                user.setFonction("");
                user.setEmail("admin@gmail.com");
                user.setPassword(bCryptPasswordEncoder.encode("123456"));
                User newUser = userService.saveUser(user);

                Set<Role> roles = new HashSet<>();
                for (String[] roleData : rolesList) {
                    Role role = roleService.findByName(roleData[0]);
                    if (role != null) {
                        roles.add(role);
                    }
                }
                newUser.setRoles(roles);
                userService.saveUser(newUser);
            }

        };
    }
//		String pltfolder = "\\\\MATNR-FP01\\Groups\\Dep\\Ing\u00E9nierie\\CAD\\PLT Files\\";
//		List<String> arrText = new ArrayList<String>();
//		BufferedReader br = null;
//		String pltName = "L002907707NCPAA";
//		String pointXY= "";
//		List<String> arrPointXY = new ArrayList<String>();
//		try {
//			br = new BufferedReader(new InputStreamReader(new FileInputStream(pltfolder+pltName+".plt"),"windows-1252"));
//			String[] liste = br.lines().collect(Collectors.toList()).toArray(new String[br.lines().collect(Collectors.toList()).size()]);
//			for(int i = 1; i < liste.length; i++){
//				System.out.println(liste[i]);
//				if (!liste[i].contains("SI")) {
//					String[] arrString = liste[i].split(";");
//					boolean puPassed = false;
//					for(int j = 0; j < arrString.length; j++) {
//						if (arrString[j].startsWith("PU")) {
//							if(pointXY.length() > 0) {
//								arrPointXY.add(pointXY);
//								pointXY = "";
//							}
//							pointXY = arrString[j].replace("PU", "");
//						}
//						if (arrString[j].startsWith("PD")) {
//							if (pointXY.length() > 0) {
//								pointXY += "," + arrString[j].replace("PD", "");
//							}
//						}
//					}
//				}
//			}
//			if (!pointXY.isEmpty()) {
//				arrPointXY.add(pointXY);
//			}
//
//			for(int i = 0; i < arrPointXY.size(); i++) {
//				System.out.println(arrPointXY.get(i));
//			}
//
//
//
//		} catch (FileNotFoundException | UnsupportedEncodingException e) {
//
//		}
}
