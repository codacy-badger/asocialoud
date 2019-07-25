package org.yardimci.asocialoud.members.controller.member;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.yardimci.asocialoud.members.controller.MemberResponse;
import org.yardimci.asocialoud.members.db.model.FollowData;
import org.yardimci.asocialoud.members.db.model.Member;
import org.yardimci.asocialoud.members.db.repository.FollowDataRepository;
import org.yardimci.asocialoud.members.db.repository.MemberRepository;

import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/api/follow")
public class FollowDataController {

    private static final Logger logger = LoggerFactory.getLogger(FollowDataController.class);

    @Autowired
    private FollowDataRepository followDataRepository;

    @Autowired
    private MemberRepository memberRepository;

    @GetMapping("/of/{userName}")
    public MemberResponse findFollowing(@PathVariable("userName") String userNameToQuery) {
        logger.info("Retrieving all following members of : " + userNameToQuery);
        MemberResponse memberResponse = new MemberResponse();

        Member owner = memberRepository.findByLoginName(userNameToQuery);

        List<FollowData> followDataList = followDataRepository.findAllByOwnerMember(owner);
        memberResponse.setData(followDataList);
        memberResponse.setStatus(HttpStatus.OK.toString());
        return memberResponse;
    }


    @PostMapping("/add/{memberToFollow}")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addToFollowers(@RequestBody Member ownerMember, @PathVariable("memberToFollow") String memberToFollow) {
        logger.info("Follow member request received");
        MemberResponse memberResponse = new MemberResponse();

        if (ownerMember == null || ownerMember.getLoginName() == null || ownerMember.getLoginName().isEmpty() || memberToFollow == null || memberToFollow.isEmpty()) {
            logger.warn("Missing member info");
            memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            memberResponse.setData("error.missinginformation");
            return memberResponse;
        }


        Member owner = memberRepository.findByLoginName(ownerMember.getLoginName());

        Member toFollow = memberRepository.findByLoginName(memberToFollow);


        boolean alreadyFollowing = owner.getFollowDataList().stream().anyMatch(f -> f.getMemberToFollow().equals(toFollow));
        if (alreadyFollowing) {
            logger.debug("Already following");
            memberResponse.setStatus(HttpStatus.EXPECTATION_FAILED.toString());
            memberResponse.setData("info.exists");
        } else {
            FollowData followData = new FollowData();
            followData.setOwner(owner);
            followData.setMemberToFollow(toFollow);
            followData.setFollowDate(new Date());
            followData.setAllowRelauding(true);

            owner.getFollowDataList().add(followData);

            try {
                logger.info("Saving member : " + ownerMember);
                memberRepository.save(owner);
                memberResponse.setStatus(HttpStatus.CREATED.toString());
                memberResponse.setData(followData);
            } catch (Exception e) {
                logger.error("Unable to save member", e);
                memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
                memberResponse.setData("error.servererror");
            }
        }

        return memberResponse;
    }


    @PostMapping("/remove/{memberToUnFollow}")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse removeFromFollowers(@RequestBody Member ownerMember, @PathVariable("memberToUnFollow") String memberToUnFollow) {
        logger.info("Unfollow member request received : " + memberToUnFollow);
        MemberResponse memberResponse = new MemberResponse();

        if (ownerMember == null || ownerMember.getLoginName() == null || ownerMember.getLoginName().isEmpty() || memberToUnFollow == null || memberToUnFollow.isEmpty()) {
            logger.warn("Missing member info");
            memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            memberResponse.setData("error.missinginformation");
            return memberResponse;
        }


        Member owner = memberRepository.findByLoginName(ownerMember.getLoginName());

        Member toUnfollow = memberRepository.findByLoginName(memberToUnFollow);

        List<FollowData> followDataList =  owner.getFollowDataList();

        FollowData followDataToRemove = null;
        for (FollowData followData : followDataList) {
            if (followData.getMemberToFollow().equals(toUnfollow)) {
                followDataToRemove = followData;
                break;
            }
        }

        if (followDataToRemove != null) {
            owner.getFollowDataList().remove(followDataToRemove);
            try {
                logger.info("Saving member : " + ownerMember);
                memberRepository.save(owner);
                memberResponse.setStatus(HttpStatus.OK.toString());
                memberResponse.setData(owner.getFollowDataList());
            } catch (Exception e) {
                logger.error("Unable to save member", e);
                memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
                memberResponse.setData("error.servererror");
            }
        } else {
            logger.info("Nobody found to unfollow for given user");
            memberResponse.setStatus(HttpStatus.BAD_REQUEST.toString());
            memberResponse.setData("error.notfound");
        }




        return memberResponse;
    }


}
