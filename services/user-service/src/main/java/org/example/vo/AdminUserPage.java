package org.example.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserPage implements Serializable {

    private List<AdminUserInfo> records;

    private long total;

    private int page;

    private int size;
}
