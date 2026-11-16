package com.example.splitbill.dto.response;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Participant;

public record GroupResponse(
        Long id,
        String name,
        List<ParticipantResponse> participants) {
    public static GroupResponse from(BillGroup group) {
        List<ParticipantResponse> participants = new ArrayList<>();

        for (Participant participant : group.getParticipants()) {
            participants.add(new ParticipantResponse(participant.getId(), participant.getName()));
        }
        participants.sort(Comparator.comparing(ParticipantResponse::id));
        return new GroupResponse(group.getId(), group.getName(), participants);
    }
}
