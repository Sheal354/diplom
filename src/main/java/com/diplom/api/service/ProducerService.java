package com.diplom.api.service;

import com.diplom.api.dto.UpdateProducerRequest;
import com.diplom.persistence.entity.ProducerEntity;
import com.diplom.persistence.repository.ProducerRepository;
import org.springframework.stereotype.Service;

/**
 * Сервис управления профилем производителя.
 * <p>
 * Предоставляет методы для получения и обновления данных производителя.
 * Все операции выполняются для того производителя, чей идентификатор передан в метод.
 */
@Service
public class ProducerService {

    private final ProducerRepository producerRepository;

    /**
     * @param producerRepository репозиторий для доступа к таблице producers
     */
    public ProducerService(ProducerRepository producerRepository) {
        this.producerRepository = producerRepository;
    }

    /**
     * Возвращает сущность производителя по его идентификатору.
     *
     * @param producerId идентификатор производителя
     * @return загруженная из БД сущность {@link ProducerEntity}
     * @throws RuntimeException если производитель с указанным id не найден
     */
    public ProducerEntity getProfile(Integer producerId) {
        return producerRepository.findById(producerId)
                .orElseThrow(() -> new RuntimeException("Producer not found"));
    }

    /**
     * Обновляет профиль производителя.
     * <p>
     * Принимает только те поля, которые были переданы (не равны {@code null}).
     * Остальные поля сущности остаются без изменений.
     *
     * @param producerId идентификатор производителя
     * @param request    новые значения полей (опциональные)
     * @return обновлённая и сохранённая сущность {@link ProducerEntity}
     * @throws RuntimeException если производитель с указанным id не найден
     */
    public ProducerEntity updateProfile(Integer producerId, UpdateProducerRequest request) {
        ProducerEntity producer = producerRepository.findById(producerId)
                .orElseThrow(() -> new RuntimeException("Producer not found"));

        // Обновляем только те поля, которые переданы (не null)
        if (request.getName() != null) producer.setName(request.getName());
        if (request.getContactPerson() != null) producer.setContactPerson(request.getContactPerson());
        if (request.getEmail() != null) producer.setEmail(request.getEmail());
        if (request.getPhone() != null) producer.setPhone(request.getPhone());
        if (request.getIsActive() != null) producer.setIsActive(request.getIsActive());

        return producerRepository.save(producer);
    }
}