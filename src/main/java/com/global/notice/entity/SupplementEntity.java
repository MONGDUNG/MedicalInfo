package com.global.notice.entity;

import java.util.List;
import java.util.Set;

import com.global.member.entity.MemberEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SUPPLEMENT")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SupplementEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    	@Column(name = "ID")
		private Long id;
	
	 	@Column(name = "PRDLST_REPORT_NO", nullable = false)
	 	private Long prdlstReportNo;

	    @Column(name = "PRDT_SHAP_CD_NM", length = 20)
	    private String prdtShapCdNm;

	    @Column(name = "LCNS_NO", nullable = false)
	    private Long lcnsNo;

	    @Column(name = "PRDLST_NM", length = 200, nullable = false)
	    private String prdlstNm;

	    @Column(name = "IFTKN_ATNT_MATR_CN", length = 2000)
	    private String iftknAtntMatrCn;

	    @Column(name = "BSSH_NM", length = 100, nullable = false)
	    private String bsshNm;

	    @Column(name = "ETC_RAWMTRL_NM", length = 2000)
	    private String etcRawmtrlNm;

	    @Column(name = "PRIMARY_FNCLTY", length = 2000)
	    private String primaryFnclty;

	    @Column(name = "CSTDY_MTHD", length = 500)
	    private String cstdyMthd;
	    
	    @Column(name = "NTK_MTHD", length = 1000)
	    private String ntkMthd;
	    
	    @Column(name = "CAP_RAWMTRL_NM", length = 500)
	    private String capRawmtrlNm;
	    
	    @Column(name = "STDR_STND", length = 5000)
	    private String stdrStnd;
	    
	    @Column(name = "POG_DAYCNT", length = 100)
	    private String pogDaycnt;
	    
	    @Column(name = "INDIV_RAWMTRL_NM", length = 2000)
	    private String indivRawmtrlNm;
	    
	    @Column(name = "RAWMTRL_NM", length = 500)
	    private String rawmtrlNm;
	    
	    @OneToMany(mappedBy="supplement" , cascade=CascadeType.REMOVE)
		private List<AnswerEntity> answerList;
	    
	    @ManyToMany
		private Set<MemberEntity> voter;
	    public int getVoterCount() {
	        return voter != null ? voter.size() : 0; // null 체크 후 size() 반환
	    }
}