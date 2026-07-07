package com.lear.MGCMS.domain;

import javax.persistence.*;

@Entity
@Table(name = "ProgrammeDistribution", uniqueConstraints = @UniqueConstraint(columnNames = {"machineCncId", "programmeNumber"}))
public class ProgrammeDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "machineCncId")
    private MachineCnc machine;

    @Column(name = "programmeNumber")
    private Integer programmeNumber;

    // Constructors
    public ProgrammeDistribution() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MachineCnc getMachine() { return machine; }
    public void setMachine(MachineCnc machine) { this.machine = machine; }

    public Integer getProgrammeNumber() { return programmeNumber; }
    public void setProgrammeNumber(Integer programmeNumber) { this.programmeNumber = programmeNumber; }
}
