package com.global.Post.Service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.global.Post.Data.PostEntity;
import com.global.Post.Data.ReplDTO;
import com.global.Post.Data.ReplEntity;
import com.global.Post.Repository.PostRepository;
import com.global.Post.Repository.ReplRepository;
import com.global.member.entity.MemberEntity;
import com.global.member.entity.MemberTierEntity;
import com.global.member.repository.MemberRepository;
import com.global.member.repository.MemberTierRepository;



@Service
public class ReplService {
	
	@Autowired
	PostRepository pr;
	
	@Autowired
	ReplRepository rr;
	
	@Autowired
	MemberRepository mr;
	
	@Autowired
	MemberTierRepository tr;
	
	public void replCreate(Integer postId, String content, String username) {
		PostEntity pe = pr.findById(postId).get(); 
		MemberEntity me = mr.findByUsername(username).get(); 		
		MemberTierEntity te = tr.findByTier(me.getMemberstatus()).get();
		ReplEntity re = ReplEntity.builder()
				.content(content)
				.createdate(LocalDateTime.now())
				.post(pe)
				.member(me)								
				.build();
				rr.save(re);
	}
	
	public void delete(Integer id) {  //삭제 . id정보 가져와서 삭제
		ReplEntity re = rr.findById(id).get();
		rr.delete(re);
	}
	
    public void modifyRepl(Integer id, String content) {
        ReplEntity repl = rr.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글을 찾을 수 없습니다."));
        repl.setContent(content);
        repl.setCreatedate(LocalDateTime.now()); // 수정 시간 업데이트
        		rr.save(repl);
    }


}
