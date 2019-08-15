package org.yardimci.asocialoud.members.controller.member;


import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.yardimci.asocialoud.members.controller.MemberResponse;
import org.yardimci.asocialoud.members.controller.exception.MemberNotFoundException;
import org.yardimci.asocialoud.members.db.model.FollowData;
import org.yardimci.asocialoud.members.db.model.Member;
import org.yardimci.asocialoud.members.db.repository.FollowDataRepository;
import org.yardimci.asocialoud.members.db.repository.MemberRepository;
import org.yardimci.asocialoud.members.dto.MemberSearchResultDto;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
//@CrossOrigin(origins = "http://localhost:6600")
public class MemberController {

    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FollowDataRepository followDataRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public MemberResponse findAll() {
        logger.info("Retrieving all members");
        MemberResponse memberResponse = new MemberResponse();

        List<Member> memberList = new ArrayList<>();

        Iterator<Member> memberIterator = memberRepository.findAll().iterator();

        memberIterator.forEachRemaining(memberList::add);

        memberResponse.setData(memberList);
        memberResponse.setStatus(HttpStatus.OK.toString());
        return memberResponse;
    }

    @GetMapping("/{userName}")
    public MemberResponse findByUserName(@PathVariable("userName") String userNameToQuery) {
        logger.info("Retrieving member by login name : "+ userNameToQuery);
        Member byLoginName = memberRepository.findByLoginName(userNameToQuery);
        MemberResponse memberResponse = new MemberResponse();
        memberResponse.setData(byLoginName);
        memberResponse.setStatus(byLoginName == null ? HttpStatus.NOT_FOUND.toString() : HttpStatus.OK.toString());
        return memberResponse;
    }

    @GetMapping("/id/{id}")
    public Member findMemberById(@PathVariable Long id) {
        logger.info("Retrieving member by id : " + id);
        Optional<Member> member = memberRepository.findById(id);

        if (!member.isPresent())
            throw new MemberNotFoundException("id-" + id);
            //return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        //return memberRepository.findById(id).orElseThrow(MemberNotFoundException::new);
        return member.get();
    }


    @GetMapping("/search/{userName}")
    public MemberResponse findAllByUserName(@PathVariable("userName") String userNameToQuery, Principal principal) {
        logger.info("Retrieving members by login name : "+ userNameToQuery);
        MemberResponse memberResponse = new MemberResponse();

        List<Member> byLoginName = memberRepository.findByLoginNameIgnoreCaseContaining(userNameToQuery);
        List<MemberSearchResultDto> searchResultList = new ArrayList<>();

        if (byLoginName != null) {
            Member ownerMember = memberRepository.findByLoginName(principal.getName());
            ModelMapper modelMapper = new ModelMapper();
            for (Member resultMember : byLoginName) {
                MemberSearchResultDto dto = modelMapper.map(resultMember, MemberSearchResultDto.class);
                dto.setFollowedByMe(followDataRepository.existsFollowDataByOwnerAndMemberToFollow(ownerMember, resultMember));
                dto.setFollowsMe(followDataRepository.existsFollowDataByOwnerAndMemberToFollow(resultMember, ownerMember));
                searchResultList.add(dto);
            }
        }

        memberResponse.setData(searchResultList);
        memberResponse.setStatus(byLoginName == null ? HttpStatus.NOT_FOUND.toString() : HttpStatus.OK.toString());
        return memberResponse;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@RequestBody Member member) {
        logger.info("Save member request received");
        MemberResponse memberResponse = new MemberResponse();

        if (member.getRealName() == null || member.getRealName().isEmpty() || member.getLoginName() == null || member.getLoginName().isEmpty() || member.getPassword()==null || member.getPassword().isEmpty()) {
            logger.warn("Missing member info");
            memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            memberResponse.setData("error.missinginformation");
            return memberResponse;
        }


        Member byLoginName = memberRepository.findByLoginName(member.getLoginName());
        if (byLoginName != null) {
            logger.warn("Member name already exists");
            memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            memberResponse.setData("error.membernameexists");
            return memberResponse;        }

        Member byEmail = memberRepository.findByEmail(member.getEmail());
        if (byEmail != null) {
            logger.warn("Member email already exists");
            memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            memberResponse.setData("error.emailistaken");
            return memberResponse;
        }

        try {
            logger.info("Saving member : " + member.getLoginName());
            member.setPassword(passwordEncoder.encode(member.getPassword()));
            memberRepository.save(member);
            memberResponse.setStatus(HttpStatus.CREATED.toString());
            memberResponse.setData(member);
        } catch (Exception e) {
            logger.error("Unable to save member", e);
            memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            memberResponse.setData("error.servererror");
        }

        return memberResponse;
    }

    // todo require admin privilege
    @DeleteMapping("/{id}")
    public MemberResponse delete(@PathVariable Long id) {
        logger.info("Deleting user with id : " + id);
        MemberResponse memberResponse = new MemberResponse();
        Member memberToDelete = memberRepository.findById(id).orElseThrow(MemberNotFoundException::new);
        try {
            //todo convert to service, introduce @transactional.

            // step 1 : delete member from other member's follow data
            logger.info("Removing follow data");
            List<FollowData> allFollowerDataOfMember = followDataRepository.findAllFollowersOfMember(memberToDelete);
            for (FollowData fd : allFollowerDataOfMember) {
                followDataRepository.delete(fd);
            }
            logger.info("Removing user");
            memberRepository.deleteById(id);
            memberResponse.setStatus(HttpStatus.OK.toString());
        } catch (Exception e) {
            logger.error("Unable to delete member : " + id, e);
            memberResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
        }
        return memberResponse;
    }

    @PutMapping("/{userName}")
    public MemberResponse updateMember(@RequestBody Member member, @PathVariable String userName) {
        logger.info("Updating user : " + userName);
        MemberResponse memberResponse = new MemberResponse();

        if (userName == null /*|| !member.getLoginName().equals(userName)*/ ||
                member == null || member.getRealName() == null || member.getRealName().isEmpty() || member.getEmail() == null || member.getEmail().isEmpty()) {
            //throw new MemberIdMismatchException("mismatch : " + userName);
            memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            memberResponse.setData("error.missinginformation");
            return memberResponse;
        }
        Member savedMember = null;
        try {
            Member userInDb = memberRepository.findByLoginName(userName);
            userInDb.setRealName(member.getRealName());
            userInDb.setEmail(member.getEmail());

            savedMember = memberRepository.save(userInDb);
            memberResponse.setData(savedMember);
            memberResponse.setStatus(HttpStatus.OK.toString());
        } catch (Exception e) {
            logger.error("Unable to update user : " + userName, e);
            memberResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
        }


        return memberResponse;
    }

}
