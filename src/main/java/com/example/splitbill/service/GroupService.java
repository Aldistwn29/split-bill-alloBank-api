package com.example.splitbill.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.splitbill.dto.request.CreateGroupRequest;
import com.example.splitbill.dto.response.GroupResponse;
import com.example.splitbill.exception.BusinessException;
import com.example.splitbill.exception.ResourceNotFoundException;
import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Participant;
import com.example.splitbill.repository.BillGroupRepository;

@Service
public class GroupService {

    private final BillGroupRepository billGroupRepository;

    public GroupService(BillGroupRepository billGroupRepository) {
        this.billGroupRepository = billGroupRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        List<String> participantNames = request.participants().stream()
                .map(String::trim)
                .toList();
        Set<String> seenNames = new HashSet<>();

        for (String name : participantNames) {
            if (!seenNames.add(name)) {
                throw new BusinessException("Participant tidak boleh memiliki duplikat");
            }
        }

        BillGroup group = new BillGroup(request.name().trim());
        for (String name : participantNames) {
            new Participant(name, group);
        }

        BillGroup savedGroup = billGroupRepository.save(group);

        return GroupResponse.from(savedGroup);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long id) {
        BillGroup group = billGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group dengan id " + id + " tidak ditemukan"));

        return GroupResponse.from(group);
    }
}