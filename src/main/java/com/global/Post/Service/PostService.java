package com.global.Post.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable; // PageRequest 관련 클래스
import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.global.Post.Data.FileDTO;
import com.global.Post.Data.FileEntity;
import com.global.Post.Data.PostDTO;
import com.global.Post.Data.PostEntity;
import com.global.Post.Data.ReplDTO;
import com.global.Post.Data.ReplEntity;
import com.global.Post.Repository.FileRepository;
import com.global.Post.Repository.PostRepository;
import com.global.member.dto.MemberDTO;
import com.global.member.entity.MemberEntity;
import com.global.member.entity.MemberTierEntity;
import com.global.member.repository.MemberRepository;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
	
	@Autowired
	PostRepository pr;
		
	private final MemberRepository mr;
	public List<ReplDTO> ReplList;
	public List<PostDTO> PostList;
	public List<FileEntity> fileList;
	private final FileRepository fr;
	@Autowired
	 private ResourceLoader resourceLoader;
	
	
	public void PostCreate(PostDTO dto, @RequestParam("save") List<MultipartFile> files ,
			@RequestParam("username")String username) {			
			MemberEntity me = mr.findByUsername(username).get();			
			PostEntity pe = PostEntity.builder()
				.subject(dto.getSubject())
				.content(dto.getContent())
				.category(dto.getCategory())								
				.member(me)
				.createDate(LocalDateTime.now())				
				.build();
		pr.save(pe);
				saveFile(files,pe);
		
	}
	
	public void saveFile(List<MultipartFile>files ,PostEntity pe ) { 
		
		 String staticUploadPath = "";
	     
		
		 if(files != null && !files.isEmpty()) {
			 this.fileList = new ArrayList<>();
			 for(MultipartFile mf : files) {
				 String ofilename = mf.getOriginalFilename();
				 if(ofilename != null && !ofilename.equals("")) {
			 		long time = System.currentTimeMillis();
			 		String filename = time + ofilename;
			 		
	 				try {
	 					staticUploadPath = resourceLoader.getResource("classpath:/static/").getFile().getAbsolutePath();
	 					System.out.println(staticUploadPath);
	 					File dir = new File(staticUploadPath+"/uploads");
	 					System.out.println(dir.mkdir()); 
	 					
	 					File dir2 = new File(staticUploadPath+"/uploads/"+filename);
	 					mf.transferTo(dir2);				 					
	 					FileEntity file = new FileEntity();
	 							file.setPost(pe);
	 							file.setFilename(filename);
	 							file.setPath("uploads");
	 					fr.save(file);
	 					fileList.add(file);
	 				}catch(Exception e) {	
	 					e.printStackTrace();
	 				}
			 	}
			 }
		 }	 
	}
	
	public long totalCount() {  //전체 수량
		return pr.count();
	}
	
	public List<PostDTO> PostPage(int page, String kw) {
	    // Sort 설정: 생성일 기준 내림차순 정렬
	    List<Sort.Order> sorts = new ArrayList<>();
	    sorts.add(Sort.Order.desc("createDate"));	   
	    Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));	
	    Specification<PostEntity> spec = search(kw);
	    Page<PostEntity> pages = pr.findAll(spec, pageable);
	    List<PostEntity> entityList = pages.toList();
	    List<PostDTO> postList = new ArrayList<>();
	    if (entityList != null && !entityList.isEmpty()) {
	        for (PostEntity entity : entityList) {
	            postList.add(entityChange(entity));
	        }
	    }
	    return postList;
	}
	
		
	
	private Specification<PostEntity> search(String kw){		
		return new Specification<>() {
			@Override
			public Predicate toPredicate(Root<PostEntity> root,  
					CriteriaQuery<?> query,  			
					CriteriaBuilder criteriaBuilder) {  
							
				query.distinct(true);
				Join<PostEntity , MemberEntity> u1 = root.join("member", JoinType.LEFT);
				Join<PostEntity , ReplEntity> a = root.join("ReplList", JoinType.LEFT);
				Join<ReplEntity , MemberEntity> u2 = root.join("member", JoinType.LEFT);
				
				return	criteriaBuilder.or(
									criteriaBuilder.like(root.get("subject"), "%"+kw+"%"),
									criteriaBuilder.like(root.get("content"), "%"+kw+"%"),
									criteriaBuilder.like(u1.get("username"), "%"+kw+"%"),
									criteriaBuilder.like(a.get("content"), "%"+kw+"%"),
									criteriaBuilder.like(u2.get("username"), "%"+kw+"%"));	
			}
			
		};
		
	}
	
	
	public List<PostDTO> findPostsByCategory(String category, int page) {
	    Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Order.desc("createDate")));
	    // 생성일 기준 내림차순
	    Page<PostEntity> pages = pr.findByCategory(category, pageable);
	    return pages.getContent().stream()
                .map(this::entityChange) // 기존 메서드 사용
                .collect(Collectors.toList());
	}

	public long totalCountByCategory(String category) {
	    return pr.countByCategory(category); // 카테고리에 따른 총 게시글 수 계산
	}

	
	
	
	
	public PostDTO entityChange(PostEntity entity) { // 질문 엔티티 >> DTO로
		 PostDTO postDTO = PostDTO.builder() 	
				.id(entity.getId())
				.subject(entity.getSubject())
				.content(entity.getContent())				
				.createDate(entity.getCreateDate())
				.category(entity.getCategory())
				.member(entityChange(entity.getMember()))
				.build();
	
		 
		 List<ReplEntity> postList= entity.getReplList(); //답글 목록 가져와서 바꾸기 작업
		 List<ReplDTO>dtoList = new ArrayList<>();
		 if(postList != null && !postList.isEmpty()) {
			 for(ReplEntity replEntity : entity.getReplList()) {
				 dtoList.add(entityChange(replEntity));  //answer dto에 entity를 set
			 } }
		 
		 
		 List<FileEntity> fileList = entity.getFileList();
		    List<FileDTO> filedtoList = new ArrayList<>();
		    if (fileList != null && !fileList.isEmpty()) {
		        for (FileEntity fileentity : entity.getFileList()) {
		            filedtoList.add(entityChange(fileentity));
		        } }
		    
		 postDTO.setFileList(filedtoList);	 
		 postDTO.setReplList(dtoList);  
		 		
		 return postDTO;		 
	}

	public MemberDTO entityChange(MemberEntity memberEntity) {  // post내부 member 내용 체우기
	    return memberEntity == null ? null : MemberDTO.builder()
	        .username(memberEntity.getUsername())
	        .email(memberEntity.getEmail())	  
	        .memberstatus(memberEntity.getMemberstatus())
	        .build();
	}

	public FileDTO entityChange(FileEntity fileentity) {		
		return fileentity == null ? null : FileDTO.builder()
				.id(fileentity.getId())
				.filename(fileentity.getFilename())
				.path(fileentity.getPath())	
				.build();
	}
	
	public ReplDTO entityChange(ReplEntity replEntity) {
		return replEntity == null ? null : ReplDTO.builder()
				.id(replEntity.getId())
				.content(replEntity.getContent())
				.createdate(replEntity.getCreatedate())
				.member(entityChange(replEntity.getMember()))
				.build();
	}	
	
	
	
	
	public String saveFile(MultipartFile mf) { // 파일 업로드작업
		System.out.println("=====saveFile=====>> "+mf);
		String filename = null;
		if(mf != null) {
			String org = mf.getOriginalFilename();
			if(org != null && !org.equals("") ) {
				long time = System.currentTimeMillis();
				filename = time+org;  //파일 이름 작업
				
				try {
					Resource resource = resourceLoader.getResource("classpath:static/uploads/"+filename);
			 		File copy = resource.getFile();
			 		System.out.println("=====sing=====>> "+copy.canExecute());
					mf.transferTo(copy); // 덮어쓰기 작업
				}catch(Exception e){
					e.printStackTrace(); 
				}
			}
		}
			return filename;
	}
	
	public void deleteFile(String filename) { // 파일 삭제
		File copy = new File("c:/upload/"+filename); 
		if(copy.exists()) {  // 유무 체크 boolean 타입. 있을시 true
			copy.delete();
		}
	}
	
	public PostDTO postId(Integer id) { // 아이디로 정보 가져오기
		PostEntity pe = pr.findById(id).get();
		return entityChange(pe);
	}
	
	public void delete(Integer id) {  //삭제 . id정보 가져와서 삭제
		PostEntity pe = pr.findById(id).get();
		pr.delete(pe);
	}
	
	
	
	public void PostModify(Integer id, PostDTO postDTO , 
			List<MultipartFile> mf ) { // 수정 업데이트
		PostEntity pe = pr.findById(id).get();
		pe.setSubject(postDTO.getSubject());
		pe.setContent(postDTO.getContent());
		pe.setCategory(postDTO.getCategory());
		pr.save(pe);
		saveFile(mf, pe);
	}
	
	
	 public void deleteImage(String filename , Integer id) { 
	        deleteFile(filename); // 개별 파일 삭제 로직 호출
	        FileEntity fe = fr.findById(id).get();
	        fr.delete(fe);
	}


}
