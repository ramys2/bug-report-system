package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.dto.component.CreateComponentRequest;
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
        var responsibleDeveloperId = UUID.randomUUID();
        var request = new CreateComponentRequest(
                "API", "Handles public endpoints", responsibleDeveloperId);
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
        assertThat(component.getResponsibleDeveloperId()).isEqualTo(request.responsibleDeveloperId());
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
        assertThat(componentCaptor.getValue().getResponsibleDeveloperId()).isNull();
        assertThat(result.id()).isEqualTo(componentId);
    }
}
