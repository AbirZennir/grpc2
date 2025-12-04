package ma.projet.grpc2.controllers;

import io.grpc.stub.StreamObserver;
import ma.projet.grpc2.entities.Compte;
import ma.projet.grpc2.services.CompteService;
import ma.projet.grpc2.stubs.CompteRequest;
import ma.projet.grpc2.stubs.CompteServiceGrpc;
import ma.projet.grpc2.stubs.GetAllComptesRequest;
import ma.projet.grpc2.stubs.GetAllComptesResponse;
import ma.projet.grpc2.stubs.GetCompteByIdRequest;
import ma.projet.grpc2.stubs.GetCompteByIdResponse;
import ma.projet.grpc2.stubs.GetTotalSoldeRequest;
import ma.projet.grpc2.stubs.GetTotalSoldeResponse;
import ma.projet.grpc2.stubs.SaveCompteRequest;
import ma.projet.grpc2.stubs.SaveCompteResponse;
import ma.projet.grpc2.stubs.SoldeStats;
import ma.projet.grpc2.stubs.TypeCompte;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class CompteServiceImpl extends CompteServiceGrpc.CompteServiceImplBase {

    private final CompteService compteService;

    public CompteServiceImpl(CompteService compteService) {
        this.compteService = compteService;
    }

    // -------- helpers de mapping --------

    // Entité JPA -> message gRPC
    private ma.projet.grpc2.stubs.Compte mapToGrpc(Compte c) {
        return ma.projet.grpc2.stubs.Compte.newBuilder()
                .setId(String.valueOf(c.getId()))
                .setSolde(c.getSolde())
                .setDateCreation(c.getDateCreation())
                .setType(TypeCompte.valueOf(c.getType()))
                .build();
    }

    // Message gRPC -> Entité JPA
    private Compte mapToEntity(CompteRequest req) {
        Compte c = new Compte();
        c.setSolde(req.getSolde());
        c.setDateCreation(req.getDateCreation());
        c.setType(req.getType().name());
        return c;
    }

    // ---------- Méthodes gRPC ----------

    @Override
    public void allComptes(GetAllComptesRequest request,
                           StreamObserver<GetAllComptesResponse> responseObserver) {

        List<Compte> comptesEntity = compteService.findAllComptes();

        List<ma.projet.grpc2.stubs.Compte> comptesGrpc = comptesEntity.stream()
                .map(this::mapToGrpc)
                .collect(Collectors.toList());

        GetAllComptesResponse response = GetAllComptesResponse.newBuilder()
                .addAllComptes(comptesGrpc)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void compteById(GetCompteByIdRequest request,
                           StreamObserver<GetCompteByIdResponse> responseObserver) {

        Long id;
        try {
            id = Long.valueOf(request.getId());
        } catch (NumberFormatException e) {
            responseObserver.onError(new RuntimeException("ID de compte invalide"));
            return;
        }

        Compte compte = compteService.findCompteById(id);

        if (compte == null) {
            responseObserver.onError(new RuntimeException("Compte non trouvé"));
            return;
        }

        GetCompteByIdResponse response = GetCompteByIdResponse.newBuilder()
                .setCompte(mapToGrpc(compte))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void totalSolde(GetTotalSoldeRequest request,
                           StreamObserver<GetTotalSoldeResponse> responseObserver) {

        List<Compte> comptes = compteService.findAllComptes();

        int count = comptes.size();
        float sum = 0;
        for (Compte c : comptes) {
            sum += c.getSolde();
        }
        float average = count > 0 ? sum / count : 0;

        SoldeStats stats = SoldeStats.newBuilder()
                .setCount(count)
                .setSum(sum)
                .setAverage(average)
                .build();

        GetTotalSoldeResponse response = GetTotalSoldeResponse.newBuilder()
                .setStats(stats)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void saveCompte(SaveCompteRequest request,
                           StreamObserver<SaveCompteResponse> responseObserver) {

        CompteRequest compteReq = request.getCompte();

        Compte entity = mapToEntity(compteReq);
        Compte saved = compteService.saveCompte(entity);

        SaveCompteResponse response = SaveCompteResponse.newBuilder()
                .setCompte(mapToGrpc(saved))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
