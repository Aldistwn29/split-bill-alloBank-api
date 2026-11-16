package com.example.splitbill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.splitbill.dto.request.CreateGroupRequest;
import com.example.splitbill.dto.response.GroupResponse;
import com.example.splitbill.exception.BusinessException;
import com.example.splitbill.exception.ResourceNotFoundException;
import com.example.splitbill.model.BillGroup;
import com.example.splitbill.model.Participant;
import com.example.splitbill.repository.BillGroupRepository;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {
    @Mock
    private BillGroupRepository billGroupRepository;

    private GroupService service() {
        return new GroupService(billGroupRepository);
    }

    private void stubSaveAssignmentIds() {
        when(billGroupRepository.save(any(BillGroup.class))).thenAnswer(invocation -> {
            BillGroup group = invocation.getArgument(0);
            ReflectionTestUtils.setField(group, "id", 1L);

            long participantId = 1;
            for (Participant participant : group.getParticipants()) {
                ReflectionTestUtils.setField(participant, "id", participantId++);
            }
            return group;
        });
    }

    @Test
    void createGroup_happyPath_persistsGroupWithAllParticipants() {
        stubSaveAssignmentIds();
        CreateGroupRequest request = new CreateGroupRequest("Trip Sukabumi", List.of("Aldi", "Sujuran", "Sudahan"));

        GroupResponse response = service().createGroup(request);

        ArgumentCaptor<BillGroup> captor = ArgumentCaptor.forClass(BillGroup.class);
        verify(billGroupRepository).save(captor.capture());

        assertThat(captor.getValue().getParticipants()).hasSize(3);
        assertThat(captor.getValue().getName()).isEqualTo("Trip Sukabumi");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.participants()).hasSize(3);

    }

    @Test
    void createGroup_duplicateName_throwsBusinessException() {
        CreateGroupRequest request = new CreateGroupRequest("Trip Sukabumi", List.of("Aldi", "Aldi"));

        assertThatThrownBy(() -> service().createGroup(request))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(billGroupRepository);
    }

    @Test
    void createGroup_duplicateAfterTrim_throwsBusinessException() {
        CreateGroupRequest request = new CreateGroupRequest("Trip Sukabumi", List.of("Aldi", "Aldi "));

        assertThatThrownBy(() -> service().createGroup(request))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(billGroupRepository);
    }

    @Test
    void createGroup_trimsParticipantNames() {
        stubSaveAssignmentIds();

        CreateGroupRequest request = new CreateGroupRequest("Trip Sukabumi", List.of(" Aldi ", " Sujuran", "Sudahan"));
        service().createGroup(request);

        ArgumentCaptor<BillGroup> captor = ArgumentCaptor.forClass(BillGroup.class);
        verify(billGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants())
                .extracting(Participant::getName)
                .containsExactly("Aldi", "Sujuran", "Sudahan");
    }

    @Test
    void getGroup_unknownId_throwsResourceNotFoundException() {
        when(billGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getGroup(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Group dengan id 99 tidak ditemukan");
    }
}
