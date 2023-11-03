package com.aerotravel.flightticketbooking.config;

import com.aerotravel.flightticketbooking.model.*;
import com.aerotravel.flightticketbooking.model.dto.AircraftDto;
import com.aerotravel.flightticketbooking.model.dto.FlightDto;
import com.aerotravel.flightticketbooking.model.dto.PassengerDto;
import com.aerotravel.flightticketbooking.model.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Configuration
@EnableAsync
public class AppConfig {

    @Bean
    public Executor taskExecutor() {
        val executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("AsyncFileUploader - ");
        executor.initialize();
        return executor;
    }

    @Bean
    public ObjectMapper provideObjectMapper(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }

    @Bean
    public ModelMapper modelMapper() {
        var mapper = new ModelMapper();

        setupPassengerMappings(mapper);
        setupFlightMappings(mapper);
        setupAircraftMappings(mapper);
        setupUserMappings(mapper);
        return mapper;
    }

    private void setupPassengerMappings(ModelMapper mapper) {
        PropertyMap<Passenger, PassengerDto> passengerToDtoMap = new PropertyMap<>() {
            protected void configure() {
                map().setFlightId(source.getFlight().getFlightId());
            }
        };
        mapper.addMappings(passengerToDtoMap);
    }

    private void setupFlightMappings(ModelMapper mapper) {
        TypeMap<Flight, FlightDto> flightTypeMap = mapper.createTypeMap(Flight.class, FlightDto.class);
        Converter<List<Passenger>, List<Long>> passengerConverter = ctx ->
                ctx.getSource()
                        .stream()
                        .map(Passenger::getPassengerId)
                        .collect(Collectors.toList());
        flightTypeMap.addMappings(mappr -> {
            mappr.using(passengerConverter).map(Flight::getPassengers, FlightDto::setPassengerIds);
            mappr.map(src -> src.getAircraft().getAircraftId(),
                    FlightDto::setAircraftId);
        });
    }

    private void setupAircraftMappings(ModelMapper mapper) {
        TypeMap<Aircraft, AircraftDto> aircraftTypeMap = mapper.createTypeMap(Aircraft.class, AircraftDto.class);
        Converter<List<Flight>, List<Long>> flightsConverter = ctx ->
                ctx.getSource()
                        .stream()
                        .map(Flight::getFlightId)
                        .collect(Collectors.toList());
        aircraftTypeMap.addMappings(mappr -> mappr.using(flightsConverter).map(Aircraft::getFlights, AircraftDto::setFlightIds));
    }

    private void setupUserMappings(ModelMapper mapper) {
        TypeMap<User, UserDto> userTypeMap = mapper.createTypeMap(User.class, UserDto.class);
        Converter<List<Role>, List<String>> rolesConverter = ctx ->
                ctx.getSource()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toList());
        userTypeMap.addMappings(mappr -> mappr.using(rolesConverter).map(User::getRoles, UserDto::setRoleNames));
    }
}
