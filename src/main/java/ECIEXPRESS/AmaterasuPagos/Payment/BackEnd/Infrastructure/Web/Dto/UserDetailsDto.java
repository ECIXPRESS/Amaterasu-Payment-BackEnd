package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Infrastructure.Web.Dto;

public record UserDetailsDto(
        String userId,
        String email,
        String name,
        String userType
) {}