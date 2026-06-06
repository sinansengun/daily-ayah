FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY backend/DailyAyah.Api.csproj backend/
RUN dotnet restore backend/DailyAyah.Api.csproj

COPY backend/ backend/
RUN dotnet publish backend/DailyAyah.Api.csproj -c Release -o /app/publish /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS final
WORKDIR /app
COPY --from=build /app/publish .

EXPOSE 8080
ENTRYPOINT ["dotnet", "DailyAyah.Api.dll"]
