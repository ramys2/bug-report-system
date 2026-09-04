package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IComponentRepository;

@ExtendWith(MockitoExtension.class)
class ComponentServiceTest {

    @Mock
    private IComponentRepository componentRepository;

    private ComponentService service;

    @BeforeEach
    void setUp() {
        service = new ComponentService(componentRepository);
    }

    @Test
    void getAllMapsComponentIdsToNames() {
        var componentId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        when(componentRepository.findAll()).thenReturn(List.of(component));
        when(component.getId()).thenReturn(componentId);
        when(component.getName()).thenReturn("API");

        var result = service.getAll();

        assertThat(result).containsExactlyEntriesOf(Map.of(componentId, "API"));
    }

    @Test
    void createBuildsAndSavesComponentWithOptionalFields() {
        var responsibleUserId = UUID.randomUUID();
        var request = new CreateComponentRequest(
                "API", "Handles public endpoints", responsibleUserId);
        var componentId = UUID.randomUUID();
        var savedComponent = org.mockito.Mockito.mock(Component.class);
        when(componentRepository.save(any(Component.class))).thenReturn(savedComponent);
        when(savedComponent.getId()).thenReturn(componentId);

        var result = service.create(request);

        var componentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentRepository).save(componentCaptor.capture());
        var component = componentCaptor.getValue();
        assertThat(component.getName()).isEqualTo(request.name());
        assertThat(component.getDescription()).isEqualTo(request.description());
        assertThat(component.getResponsibleUserId()).isEqualTo(request.responsibleUserId());
        assertThat(result.id()).isEqualTo(componentId);
        assertThat(result.message()).isEqualTo("Successfully created!");
    }

    @Test
    void createAllowsMissingOptionalFields() {
        var request = new CreateComponentRequest("API", null, null);
        var componentId = UUID.randomUUID();
        var savedComponent = org.mockito.Mockito.mock(Component.class);
        when(componentRepository.save(any(Component.class))).thenReturn(savedComponent);
        when(savedComponent.getId()).thenReturn(componentId);

        var result = service.create(request);

        var componentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentRepository).save(componentCaptor.capture());
        assertThat(componentCaptor.getValue().getDescription()).isNull();
        assertThat(componentCaptor.getValue().getResponsibleUserId()).isNull();
        assertThat(result.id()).isEqualTo(componentId);
    }

    @Test
    void updateNameChangesTheLoadedComponent() {
        var componentId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        var request = new UpdateComponentNameRequest("Frontend");
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(component.getId()).thenReturn(componentId);

        var response = service.updateName(componentId, request);

        verify(component).setName("Frontend");
        assertThat(response.id()).isEqualTo(componentId);
        assertThat(response.message()).isEqualTo("Component updated successfully!");
    }

    @Test
    void updateDescriptionChangesTheLoadedComponent() {
        var componentId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        var request = new UpdateComponentDescriptionRequest("Handles the UI");
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(component.getId()).thenReturn(componentId);

        var response = service.updateDescription(componentId, request);

        verify(component).setDescription("Handles the UI");
        assertThat(response.id()).isEqualTo(componentId);
    }

    @Test
    void updateResponsibleUserIdChangesTheLoadedComponent() {
        var componentId = UUID.randomUUID();
        var responsibleUserId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        var request = new UpdateComponentResponsibleUserRequest(responsibleUserId);
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(component.getId()).thenReturn(componentId);

        var response = service.updateResponsibleUserId(componentId, request);

        verify(component).setResponsibleUserId(responsibleUserId);
        assertThat(response.id()).isEqualTo(componentId);
    }

    @Test
    void updateNameThrowsWhenComponentDoesNotExist() {
        var componentId = UUID.randomUUID();
        when(componentRepository.findById(componentId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.updateName(componentId, new UpdateComponentNameRequest("Frontend")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Component with id=%s does not exist!".formatted(componentId));
    }
}
