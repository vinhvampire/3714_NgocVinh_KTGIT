package com.Group117.hrm_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String nguoiNhan;
    private String tieuDe;
    private String noiDung;
    private String loai;
    private boolean daDoc;
    private LocalDateTime ngayTao;
    private String refPayload;
}
