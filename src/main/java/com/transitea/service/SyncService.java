package com.transitea.service;

import com.transitea.dto.request.SyncUploadRequete;
import com.transitea.dto.response.SyncDownloadReponse;
import com.transitea.dto.response.SyncUploadReponse;
import com.transitea.entity.Utilisateur;

import java.time.LocalDateTime;

public interface SyncService {

    SyncUploadReponse synchroniserUpload(SyncUploadRequete requete, Utilisateur utilisateur);

    SyncDownloadReponse synchroniserDownload(LocalDateTime depuis, Utilisateur utilisateur);
}
