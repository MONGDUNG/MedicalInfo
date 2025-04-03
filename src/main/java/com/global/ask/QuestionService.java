package com.global.ask;

import java.io.File;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {
	private final QuestionRepository questionRepository;
	private final MemberRepository siteUserRepository;
	
	private List<QuestionDTO> questionList;
	private List<AnswerDTO2> answerList;
	
	public void questionModify(Integer id , QuestionDTO questionDTO , MultipartFile mf) { //수정을 위한 메서드
		// JPA는 save()가 insert와 update의 기능을 둘 다 한다.
		QuestionEntity qe = questionRepository.findById(id).get(); //findById로 검색을 해와서 qe에 모든 내용을 다 넣는다.
		qe.setSubject(questionDTO.getSubject()); //questionDTO에서 subject를 가져와서 qe에 넣어준다.
		qe.setContent(questionDTO.getContent());
		qe.setModifyDate(LocalDateTime.now());
		
		// visibility 수정 로직 추가
        qe.setVisibility(questionDTO.getVisibility());  // 수정 시 visibility도 함께 업데이트

		
		String filename = saveFile(mf); //수정할 시 새로운 파일 업로드
		if(filename != null) {
			deleteFile(qe.getFilename()); //밑에 deleteFile메서드에서 가져오는것.
			qe.setFilename(filename); //가져온 파일에 새로운 파일을 넣는다. 파일을 올리면 내 pc->upload 파일에 들어가는데 수정하면 기존 파일은 없어지고 새 파일이 올라간다.
		}
		
		questionRepository.save(qe); //insert가 아닌 update를 함
	}
	
	public void questionDelete(Integer id) { //삭제를 위한 메서드
		QuestionEntity qe = questionRepository.findById(id).get();
		questionRepository.delete(qe); //findById로 가져온걸 delete에게 넘겨주는것
	}
	
	public QuestionDTO questionId(Integer id) {
		QuestionEntity qe = questionRepository.findById(id).get(); //get을 해야 Entity로 리턴함. 여기까지가 optional. 내부에서 entity를 꺼내는것
		return entityChange(qe);
	}
	
	public void questionCreate(QuestionDTO questionDTO ,
								String username , //로그인한 사람의 정보를 가져옴
								MultipartFile mf) {
		MemberEntity su = siteUserRepository.findByUsername(username).get(); //첨부파일 처리하고 사용자 유저네임 가져오고
		QuestionEntity qe = QuestionEntity.builder()
				.subject(questionDTO.getSubject())
				.content(questionDTO.getContent())
				.createDate(LocalDateTime.now())
				.filename(saveFile(mf))
				.siteUser(su)
				.visibility(questionDTO.getVisibility()) // visibility 설정
				.build();
		
		questionRepository.save(qe); //현재 글을 써서 들어갔으니 그대로 다시 나온다는 메서드
	}
	
	public long totalCount() {
		return questionRepository.count();
	}
		
	
	public List<QuestionDTO> questionPage(int page , String kw) { //게시판 밑에 있는 페이지 번호
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createDate"));
		/*sorts.add(Sort.Order.desc("학년"));
		sorts.add(Sort.Order.desc("반"));
		sorts.add(Sort.Order.desc("번호"));  세부적으로 정렬*/
		
		Pageable pageable = PageRequest.of(page, 10 , Sort.by(sorts)); //1~10  11~20  같이 페이지를 10개씩 끊는다는 뜻
		
		Specification<QuestionEntity> spec = search(kw);
		
		Page<QuestionEntity> pages = questionRepository.findAll(spec , pageable);
		
		List<QuestionEntity> entityList = pages.toList();
		this.questionList = new ArrayList<>(); //데이터가 아예 없는 null을 받고 list를 만드려 하면 오류가 뜨므로 여기서 일단 list를 만든 뒤	
		if(!entityList.isEmpty()) {
			//this.questionList = new ArrayList<>(); list생성 코드인 이것이 여기 있으면 데이터를 받고 list를 생성해서 null데이터를 받으면 생성을 못하는 오류가 생긴다.
			for(QuestionEntity entity : entityList) {
				this.questionList.add(entityChange(entity)); //여기서 null 값도 받는다.
			}
		}
		return questionList;
	}
	
	//Specification - JPA 동적 쿼리문 작성시 사용
	private Specification<QuestionEntity> search(String kw){
		return new Specification<>() {
			@Override
			public Predicate toPredicate(Root<QuestionEntity> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) { //root 저장할 객체. query 동적 쿼리문을 만들 객체. criteriaBuilder 쿼리문 실행 객체
				// TODO Auto-generated method stub
				query.distinct(true); //중복제거
				
				Join<QuestionEntity , MemberEntity> u1 = root.join("siteUser", JoinType.LEFT);
				Join<QuestionEntity , AnswerEntity2> a = root.join("answerList" , JoinType.LEFT);
				Join<AnswerEntity2 , MemberEntity> u2 = root.join("siteUser" , JoinType.LEFT);
				
				return criteriaBuilder.or( //동적으로 작성하지만 sql보다 복잡할수도 있음. sql코드를 몰라도 되긴 하지만 적당히 알긴 알아야함. 
						criteriaBuilder.like(root.get("subject"),"%"+kw+"%") ,
						criteriaBuilder.like(root.get("content"),"%"+kw+"%") ,
						criteriaBuilder.like(u1.get("username"),"%"+kw+"%") ,
						criteriaBuilder.like(a.get("content"),"%"+kw+"%") ,
						criteriaBuilder.like(u2.get("username"),"%"+kw+"%")
						);
			}
		};
	}
	
	
	public List<QuestionDTO> questionAll(){
		List<QuestionEntity> entityList = questionRepository.findAll(); //finaAll(select * from question)\과 동일)을 이용해서 DB에있는 데이터를 리턴을 받고 리턴해줄떄 QuestionEntity리스트에 entity 파라미터들을 다 넣어줌
		if(!entityList.isEmpty()) {
			this.questionList = new ArrayList<>();
			for(QuestionEntity entity : entityList) { //entityList에서 꺼내서 entity에 넣고(Id부터 수정날짜까지 전부 다)
				this.questionList.add(entityChange(entity)); //entity를 entityChange에게 작업을 하라 보낸것
			}
		}
		return questionList;
	}
	
	public QuestionDTO entityChange(QuestionEntity entity) { //entity에 있는걸 db에서 꺼내서 DTO로 바꿔줌
		QuestionDTO questionDTO = QuestionDTO.builder()  //Question 내용들을 questionDTO에 대입함
				.id(entity.getId())
				.subject(entity.getSubject())
				.content(entity.getContent())
				.filename(entity.getFilename()) //이거 안쓰면 파일네임이 안불러와진다.
				.username(entity.getSiteUser().getUsername()) //리스트 안에있는 siteuser안에있는 username을 꺼냄
				.createDate(entity.getCreateDate())
				.modifyDate(entity.getModifyDate())
				.visibility(entity.getVisibility())  // visibility 포함
				.build(); //여기는 순서가 상관없다.
		
		List<AnswerEntity2> answerList = entity.getAnswerList(); //답변이 몇개있는지 가져옴
		List<AnswerDTO2> dtoList = new ArrayList<>();
		if(!answerList.isEmpty()) { //null일시 if문 작동. 여기서 null을 처리해서 위에는 null을 쓰지 않는다.
			for(AnswerEntity2 answerEntity: answerList) {
				dtoList.add(entityChange(answerEntity));
			}
		}
		questionDTO.setAnswerList(dtoList);
		return questionDTO; //QuestionEntity를 받아서 QuestionDTO로 바꾸고 QuestionDTO로 보내줌
	}
	
	public AnswerDTO2 entityChange(AnswerEntity2 entity) { //Answer 내용들을 대입함
		return AnswerDTO2.builder()
				.id(entity.getId())
				.content(entity.getContent())
				.createDate(entity.getCreateDate())
				.modifyDate(entity.getModifyDate())
				.build(); //AnswerEntity를 받아서 AnswerDTO로 바꾸고 AnswerDTO로 보내줌
	}
	
	public String saveFile(MultipartFile mf) {
		String filename = null;
		if(mf != null) { //파일이 없으면 여기가 null이 돼서 실행이 안되고 수정이 이루어지지 않는다.
			String org = mf.getOriginalFilename();
			if(org != null && !org.equals("")) {
				long time = System.currentTimeMillis();
				filename = time+org;
				File copy = new File("c:/upload/"+filename); //해당 파일이 비어있을시
				try {
					mf.transferTo(copy); //얘가 파일 내용을 채워준다. 사진 등등
				}catch(Exception e) { e.printStackTrace(); }
			}
		}
		return filename;
	}
	
	public void deleteFile(String filename) { //수정할때 원본 파일을 삭제하는 메서드
		File copy = new File("c:/upload/"+filename);
		if(copy.exists()) { //파일의 존재 유무를 확인함
			copy.delete(); //파일이 있을 시 동작하여 삭제됨
		}
	}
	
	
	
}
