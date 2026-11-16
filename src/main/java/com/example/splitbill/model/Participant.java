package com.example.splitbill.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "participants", uniqueConstraints = {
    @UniqueConstraint (columnNames = {"group_id", "name"})
})
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="group_id", nullable = false)
    private BillGroup group;


    protected Participant(){

    }

    public Participant(String name, BillGroup group){
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nama participant tidak boleh kosong");
        }
        if (group == null) {
            throw new IllegalArgumentException("Group tidak boleh null");
        }
        this.name = name;
        group.addParticipant(this);
    }

    void assignToGroup(BillGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("Group tidak boleh null");
        }
        if (this.group != null && !this.group.hasSameIdentity(group)) {
            throw new IllegalArgumentException("Participant tidak boleh dipindahkan ke group lain");
        }
        this.group = group;
    }

    public Long getId() {
        return id;
    }

    public String getName(){
        return name;
    }

    public BillGroup getGroup(){
        return group;
    }

}
