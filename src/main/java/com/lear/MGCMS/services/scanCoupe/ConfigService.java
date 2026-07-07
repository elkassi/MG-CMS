package com.lear.MGCMS.services.scanCoupe;

import com.lear.MGCMS.domain.scanCoupe.Config;
import com.lear.MGCMS.repositories.scanCoupe.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {
	
	@Autowired
	private ConfigRepository repo;
	
	public Config findByParam(String param) {
		return repo.findByParam(param);
	}

	public Config save(Config config) {
		return repo.save(config);
	}
	
}
