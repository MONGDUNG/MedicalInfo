FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /source
COPY dotnet/MedicalInfo.Web/MedicalInfo.Web.csproj dotnet/MedicalInfo.Web/
RUN dotnet restore dotnet/MedicalInfo.Web/MedicalInfo.Web.csproj
COPY dotnet/MedicalInfo.Web/ dotnet/MedicalInfo.Web/
RUN dotnet publish dotnet/MedicalInfo.Web/MedicalInfo.Web.csproj -c Release --no-restore -o /app /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app
COPY --from=build /app .
ENV ASPNETCORE_HTTP_PORTS=8080
ENV State__Path=/state
RUN mkdir /state && chown $APP_UID:$APP_UID /state && chmod 700 /state
USER $APP_UID
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 CMD ["dotnet", "MedicalInfo.Web.dll", "--health-check"]
ENTRYPOINT ["dotnet", "MedicalInfo.Web.dll"]
