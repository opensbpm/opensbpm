package at.softwaremacherei.jsbpm.webui.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import at.softwaremacherei.jsbpm.webui.backend.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
