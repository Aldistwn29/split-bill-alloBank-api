package com.example.splitbill.dto.response;

import com.example.splitbill.model.Participant;

public record ParticipantResponse(Long id, String name) {
    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(participant.getId(), participant.getName());
    }
}
